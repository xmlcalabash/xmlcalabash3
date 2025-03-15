package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class GraphVisualization private constructor(val graph: Graph, private val filenameMap: Map<String,String>) {
    companion object {
        fun build(graph: Graph, root: PipelineModel, filenameMap: Map<String,String>): XdmNode {
            val vis = GraphVisualization(graph, filenameMap)
            vis.build(root)
            val xml = vis.describe(root)
            return xml
        }
    }

    private lateinit var stepConfig: InstructionConfiguration
    private lateinit var gPrefix: String
    private lateinit var builder: SaxonTreeBuilder
    private val gnames = mutableMapOf<String, QName>()
    private var nextId = 1

    private val nodes = mutableListOf<Node>()
    private val modelMap = mutableMapOf<Model,Node>()
    private val portMap = mutableMapOf<ModelPort, ModelPortNode>()

    private fun describe(root: PipelineModel): XdmNode {
        stepConfig = root.step.stepConfig

        builder = SaxonTreeBuilder(root.step.stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsDescription.g("graph", gPrefix),
            stepConfig.stringAttributeMap(mapOf("filename" to filenameMap[root.step.id])),
            modelMap[root]!!.nsmap)

        for (node in nodes.filterIsInstance<InputNode>()) {
            node.describe()
        }

        for (node in nodes.filterIsInstance<OutputNode>()) {
            node.describe()
        }

        val rootNode = modelMap[root]!!
        rootNode.describe()

        for ((model, node) in modelMap) {
            if (model !== root && model is CompoundModel) {
                node.describe()
            }
        }

        for (node in nodes.filterIsInstance<InputNode>()) {
            val to = portMap[node.port]!!
            val attr = mutableMapOf<String, String?>(
                "from" to node.id,
                "to" to to.id,
                "to-step" to node.port.parent.id,
                "to-port" to node.port.name
            )
            builder.addStartElement(NsDescription.g("edge", gPrefix), stepConfig.stringAttributeMap(attr), modelMap[root]!!.nsmap)
            builder.addEndElement()
        }

        for (node in nodes.filterIsInstance<OutputNode>()) {
            val from = portMap[node.port]!!
            val attr = mutableMapOf<String, String?>(
                "from" to from.id,
                "from-step" to node.port.parent.id,
                "from-port" to node.port.name,
                "to" to node.id,
            )
            builder.addStartElement(NsDescription.g("edge", gPrefix), stepConfig.stringAttributeMap(attr), modelMap[root]!!.nsmap)
            builder.addEndElement()
        }

        for (edge in graph.edges) {
            val from = portMap[edge.from.outputs[edge.outputPort]]!!
            val to = portMap[edge.to.inputs[edge.inputPort]]!!
            val attr = mutableMapOf<String, String?>(
                "from" to from.id,
                "to" to to.id,
                "from-step" to edge.from.id,
                "from-port" to edge.outputPort,
                "to-step" to edge.to.id,
                "to-port" to edge.inputPort,
                "implicit" to (if (edge.implicit) "true" else null)
            )
            builder.addStartElement(NsDescription.g("edge", gPrefix), stepConfig.stringAttributeMap(attr), modelMap[root]!!.nsmap)
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    private fun build(root: PipelineModel) {
        val allNamespacePrefixes = mutableSetOf<String>()
        for (model in graph.models) {
            allNamespacePrefixes.addAll(model.step.inscopeNamespaces.keys)
        }
        var gp = "g"
        while (allNamespacePrefixes.contains(gp)) {
            gp = "${gp}_"
        }
        gPrefix = gp

        findPorts(root)

        for (model in graph.models) {
            when (model) {
                is AtomicModel -> addNode(AtomicStepNode(model))
                is SubpipelineModel -> addNode(SubpipelineNode(model))
                is PipelineModel -> addNode(PipelineNode(model))
                is CompoundModel -> addNode(CompoundNode(model))
                else -> throw IllegalArgumentException("Model contains unexpected model: ${model}")
            }
        }

        for (model in graph.models.filter { it !is CompoundModel }) {
            val node = modelMap[model]!!
            val pnode = if (model is SubpipelineModel) {
                if (model.parent!!.parent == null) {
                    // Ignore the top-level declare pipeline
                    null
                } else {
                    modelMap[model.parent.parent]!!
                }
            } else {
                modelMap[model.parent]!!
            }

            if (pnode != null) {
                node.parent = pnode
                pnode.children.add(node)
            }
        }

        for ((name, port) in root.inputs) {
            val inode = InputNode(port)
            addNode(inode)
        }

        for ((name, port) in root.outputs) {
            val onode = OutputNode(port)
            addNode(onode)
        }
    }
    private fun findPorts(model: Model) {
        when (model) {
            is CompoundModel -> {
                findPorts(model.head)
                findPorts(model.foot)
                for (input in model.inputs.values) {
                    ModelPortNode(input)
                }
                for (output in model.outputs.values) {
                    ModelPortNode(output)
                }
                for (child in model.children) {
                    findPorts(child)
                }
            }
            is AtomicModel, is SubpipelineModel, is Head, is Foot -> {
                for (input in model.inputs.values) {
                    ModelPortNode(input)
                }
                for (output in model.outputs.values) {
                    ModelPortNode(output)
                }
                if (model is SubpipelineModel) {
                    findPorts(model.model)
                }
            }
            else -> Unit
        }
    }

    private fun addNode(node: Node) {
        nodes.add(node)
        when (node) {
            is HeadNode -> modelMap[(node.model as CompoundModel).head] = node
            is FootNode -> modelMap[(node.model as CompoundModel).foot] = node
            is ModelNode -> modelMap[node.model] = node
            else -> Unit
        }
    }

    // ============================================================

    abstract inner class Node(val model: Model) {
        internal var parent: Node? = null
        internal var children = mutableListOf<Node>()
        abstract val id: String
        var nsmap: NamespaceMap = NamespaceMap.emptyMap()

        init {
            nsmap = nsmap.put(gPrefix, NsDescription.namespace)
            for ((prefix, uri) in model.step.inscopeNamespaces) {
                nsmap = nsmap.put(prefix, uri)
            }
        }

        fun startElement(name: QName) {
            builder.addStartElement(name, EmptyAttributeMap.getInstance(), nsmap)
        }

        fun startElement(name: QName, attr: Map<String,String?>) {
            builder.addStartElement(name, stepConfig.stringAttributeMap(attr), nsmap)
        }

        fun endElement() {
            builder.addEndElement()
        }

        fun text(text: String) {
            builder.addText(text)
        }

        abstract fun describe()
    }

    abstract inner class ModelNode(model: Model): Node(model) {
        override val id = model.id

        override fun toString(): String {
            return model.toString()
        }
    }

    inner class ModelPortNode(port: ModelPort) {
        val id = "_${nextId++}"
        val name = port.name
        val sequence = port.sequence
        val primary = port.primary
        val weldedShut = port.weldedShut

        init {
            portMap[port] = this
        }

        override fun toString(): String {
            return "${name} / ${id}"
        }
    }

    abstract inner class StepNode(model: Model): ModelNode(model) {
        val inputs = mutableListOf<ModelPortNode>()
        val outputs = mutableListOf<ModelPortNode>()

        fun describePorts(ports: List<ModelPortNode>) {
            for (port in ports) {
                startElement(NsDescription.g("port", gPrefix), mapOf(
                    "id" to port.id,
                    "primary" to "${port.primary}",
                    "sequence" to "${port.sequence}",
                    "welded-shut" to (if (port.weldedShut) "true" else null)
                ))
                text(port.name)
                endElement()
            }

        }
    }

    open inner class AtomicNode(model: Model): StepNode(model) {
        override fun describe() {
            val optName = if (model.step is AtomicExpressionStepInstruction) {
                model.step.externalName?.toString()
            } else {
                null
            }

            val optExpression = if (model.step is AtomicExpressionStepInstruction) {
                model.step.expression.toString()
            } else {
                null
            }

            startElement(NsDescription.g("atomic-step", gPrefix), mapOf(
                "id" to id,
                "type" to model.step.instructionType.toString(),
                "name" to model.step.name,
                "filename" to filenameMap[model.step.declId],
                "option-name" to optName,
                "expression" to optExpression))

            startElement(NsDescription.g("inputs", gPrefix))
            describePorts(inputs)
            endElement()
            startElement(NsDescription.g("outputs", gPrefix))
            describePorts(outputs)
            endElement()

            endElement()
        }
    }

    open inner class HeadNode(model: CompoundModel): StepNode(model) {
        override val id = "head_${model.head.id}"

        init {
            for ((name, port) in model.inputs) {
                // "current" on for-each and viewport and !context on if/choose are anomalies.
                // They're inputs because their children can read it; but it isn't an input in the same
                // way that !source is...
                when (name) {
                    "current" -> {
                        if (model.step.instructionType != NsP.forEach && model.step.instructionType != NsP.viewport) {
                            inputs.add(portMap[port]!!)
                        }
                    }
                    "!context" -> {
                        if (model.step.instructionType != NsP.`if` && model.step.instructionType != NsP.choose) {
                            inputs.add(portMap[port]!!)
                        }
                    }
                    else -> {
                        inputs.add(portMap[port]!!)
                    }
                }
            }
            for ((name, port) in model.head.outputs) {
                outputs.add(portMap[port]!!)
            }
        }

        override fun describe() {
            startElement(NsDescription.g("head", gPrefix))

            startElement(NsDescription.g("inputs", gPrefix))
            describePorts(inputs)
            endElement()
            startElement(NsDescription.g("outputs", gPrefix))
            describePorts(outputs)
            endElement()

            endElement()
        }
    }

    open inner class FootNode(model: CompoundModel): AtomicNode(model) {
        override val id = "foot_${model.head.id}"

        init {
            for ((name, port) in model.foot.inputs) {
                inputs.add(portMap[port]!!)
            }
            for ((name, port) in model.outputs) {
                outputs.add(portMap[port]!!)
            }
        }

        override fun describe() {
            startElement(NsDescription.g("foot", gPrefix))

            startElement(NsDescription.g("inputs", gPrefix))
            describePorts(inputs)
            endElement()
            startElement(NsDescription.g("outputs", gPrefix))
            describePorts(outputs)
            endElement()

            endElement()
        }
    }

    open inner class AtomicStepNode(model: AtomicModel): AtomicNode(model) {
        init {
            for ((name, port) in model.inputs) {
                inputs.add(portMap[port]!!)
            }
            for ((name, port) in model.outputs) {
                outputs.add(portMap[port]!!)
            }
        }
    }

    inner class SubpipelineNode(model: SubpipelineModel): AtomicNode(model) {
        init {
            for ((name, port) in model.inputs) {
                inputs.add(portMap[port]!!)
            }
            for ((name, port) in model.outputs) {
                outputs.add(portMap[port]!!)
            }
        }

        override fun describe() {
            startElement(NsDescription.g("subpipeline", gPrefix), mapOf(
                "id" to id,
                "ref" to (model as SubpipelineModel).model.id,
                "type" to model.step.instructionType.toString(),
                "name" to model.step.name))

            startElement(NsDescription.g("inputs", gPrefix))
            describePorts(inputs)
            endElement()
            startElement(NsDescription.g("outputs", gPrefix))
            describePorts(outputs)
            endElement()

            endElement()
        }
    }

    open inner class CompoundNode(model: CompoundModel): StepNode(model) {
        val head = HeadNode(model)
        val foot = FootNode(model)
        init {
            addNode(head)
            addNode(foot)
        }

        override fun describe() {
            startElement(NsDescription.g("compound-step", gPrefix), mapOf(
                "id" to id,
                "type" to model.step.instructionType.toString(),
                "name" to model.step.name))
            head.describe()
            for (child in children) {
                child.describe()
            }
            foot.describe()
            endElement()
        }
    }

    open inner class PipelineNode(model: PipelineModel): CompoundNode(model) {
        override fun describe() {
            val baseUri = model.step.stepConfig.baseUri?.toString()
            val type = (model.step as DeclareStepInstruction).type?.toString()

            startElement(NsDescription.g("declare-step", gPrefix), mapOf(
                "base-uri" to baseUri,
                "type" to type,
                "id" to id,
                "name" to model.step.name))
            head.describe()
            for (child in children) {
                child.describe()
            }
            foot.describe()
            endElement()
        }
    }

    inner class InputNode(val port: ModelPort): Node(port.parent) {
        override val id = "input_${nextId++}"

        override fun describe() {
            startElement(NsDescription.g("input", gPrefix))
            startElement(NsDescription.g("port", gPrefix), mapOf(
                "id" to id,
                "primary" to "${port.primary}",
                "sequence" to "${port.sequence}",
                "welded-shut" to (if (port.weldedShut) "true" else null)
            ))
            text(port.name)
            endElement()
            endElement()
        }

        override fun toString(): String {
            return id
        }
    }

    inner class OutputNode(val port: ModelPort): Node(port.parent) {
        override val id = "output_${nextId++}"

        override fun describe() {
            startElement(NsDescription.g("output", gPrefix))
            startElement(NsDescription.g("port", gPrefix), mapOf(
                "id" to id,
                "primary" to "${port.primary}",
                "sequence" to "${port.sequence}",
                "welded-shut" to (if (port.weldedShut) "true" else null)
            ))
            text(port.name)
            endElement()
            endElement()
        }

        override fun toString(): String {
            return id
        }
    }

    private data class Port(val node: Node, val port: ModelPort) {
        override fun toString(): String {
            return "${node.id}/${port}"
        }
    }

    private data class Edge(val from: Port, val to: Port, val implicit: Boolean) {
        override fun toString(): String {
            return "${from}->${to}"
        }
    }
}