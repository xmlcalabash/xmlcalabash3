package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

class   GraphVisualization private constructor(val graph: Graph) {
    companion object {
        fun build(graph: Graph, root: PipelineModel): XdmNode {
            val vis = GraphVisualization(graph)
            vis.build(root)
            val xml = vis.describe(root)
            return xml
        }
        private var sinkid = 0
    }

    private lateinit var stepConfig: InstructionConfiguration
    private val nodes = mutableListOf<Node>()
    private val modelMap = mutableMapOf<Model,Node>()
    private val edges = mutableListOf<Edge>()
    private val connMap = mutableMapOf<Port,MutableList<Port>>()
    private val connReverseMap = mutableMapOf<Port,Port>()

    private fun describe(root: PipelineModel): XdmNode {
        stepConfig = root.step.stepConfig

        val builder = SaxonTreeBuilder(root.step.stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsDescription.graph)

        for (node in nodes.filterIsInstance<InputNode>()) {
            node.describe(builder)
        }

        val rootNode = modelMap[root]!!
        rootNode.describe(builder)

        for ((model, node) in modelMap) {
            if (model !== root && model is CompoundModel) {
                node.describe(builder)
            }
        }

        for (node in nodes.filterIsInstance<OutputNode>()) {
            node.describe(builder)
        }

        for ((from, toList) in connMap) {
            for (to in toList) {
                if (to.node is SinkNode) {
                    builder.addStartElement(NsDescription.edge, stepConfig.stringAttributeMap(mapOf(
                        "from" to from.node.id,
                        "output" to from.port,
                        "to" to to.node.id
                    )))
                } else {
                    builder.addStartElement(NsDescription.edge, stepConfig.stringAttributeMap(mapOf(
                        "from" to from.node.id,
                        "output" to from.port,
                        "to" to to.node.id,
                        "input" to to.port
                    )))
                }
                builder.addEndElement()
            }
        }

        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    private fun build(root: PipelineModel) {
        for (model in graph.models) {
            when (model) {
                is AtomicModel -> addNode(AtomicStepNode(model))
                is SubpipelineModel -> addNode(SubpipelineNode(model))
                is CompoundModel -> addNode(CompoundNode(model))
                else -> throw IllegalArgumentException("Model contains unexpected model: ${model}")
            }
        }

        for (model in graph.models) {
            if (model !is CompoundModel) { // compound models are stand-alone in the graph
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
        }

        for ((name, _) in root.inputs) {
            val inode = InputNode(name)
            addNode(inode)
            addEdge(Edge(Port(inode, ""), Port(modelMap[root]!!, name)))
        }

        for ((name, _) in root.outputs) {
            val onode = OutputNode(name)
            addNode(onode)
            addEdge(Edge(Port(modelMap[root]!!, name), Port(onode, "")))
        }

        for (edge in graph.edges) {
            val fromPort = Port(modelMap[edge.from]!!, edge.outputPort)
            val toPort = Port(modelMap[edge.to]!!, edge.inputPort)
            addEdge(Edge(fromPort, toPort))
        }

        // Use toList() to make a copy to avoid concurrent modification
        for (node in nodes.toList()) {
            node.addSinks()
        }
    }

    private fun addNode(node: Node) {
        nodes.add(node)
        if (node is ModelNode) {
            //println("${node.model} == ${node}")
            modelMap[node.model] = node
        }
    }

    private fun addEdge(edge: Edge) {
        edges.add(edge)

        if (!connMap.containsKey(edge.from)) {
            connMap[edge.from] = mutableListOf()
        }

        connMap[edge.from]!!.add(edge.to)
        connReverseMap[edge.to] = edge.from
    }

    // ============================================================

    abstract inner class Node() {
        internal var parent: Node? = null
        internal var children = mutableListOf<Node>()
        abstract val id: String
        abstract fun addSinks()
        abstract fun describe(builder: SaxonTreeBuilder)
    }

    inner class SinkNode(): Node() {
        override val id = "sink_${++sinkid}";
        override fun addSinks() {
            // nop
        }
        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.sink, stepConfig.stringAttributeMap(mapOf(
                "id" to id
            )))
            builder.addStartElement(NsDescription.input, stepConfig.stringAttributeMap(mapOf("port" to "source")))
            builder.addEndElement()
            builder.addEndElement()
        }
    }

    abstract inner class ModelNode(val model: Model): Node() {
        override val id = if (model.step.name.startsWith("!")) {
            "M${model.step.id}"
        } else {
            "${model.step.name}_${model.step.id}"
        }

        override fun addSinks() {
            val pnode = model.parent.let { modelMap[it] }
            for ((name, port) in model.outputs) {
                var sendsTo: Port? = null
                for ((from, toList) in connMap) {
                    for (to in toList) {
                        if (from.node == this && from.port == name) {
                            sendsTo = to
                            break
                        }
                    }
                }
                if (sendsTo != null) {
                    continue
                }

                val sink = SinkNode()
                sink.parent = pnode!!
                pnode.children.add(sink)
                val fromPort = Port(modelMap[port.parent]!!, name)
                val toPort = Port(sink, "source")
                addNode(sink)
                addEdge(Edge(fromPort, toPort))
            }
        }

        fun connections(builder: SaxonTreeBuilder) {
            for ((name, input) in model.inputs) {
                val attr = mutableMapOf<String, String>()
                attr["port"] = name
                if (input.weldedShut) {
                    attr["welded-shut"] = "true"
                }
                builder.addStartElement(NsDescription.input, stepConfig.stringAttributeMap(attr))
                builder.addEndElement()
            }
            for ((name, output) in model.outputs) {
                val attr = mutableMapOf<String, String>()
                attr["port"] = name
                if (output.weldedShut) {
                    attr["welded-shut"] = "true"
                }
                builder.addStartElement(NsDescription.output, stepConfig.stringAttributeMap(attr))
                builder.addEndElement()
            }
        }

        override fun toString(): String {
            return model.toString()
        }
    }

    open inner class AtomicNode(model: Model): ModelNode(model) {
        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.atomic, stepConfig.stringAttributeMap(mapOf(
                "id" to id,
                "tag" to model.step.instructionType.toString(),
                "name" to model.step.name)))
            connections(builder)
            builder.addEndElement()
        }
    }

    open inner class HeadNode(model: Model): AtomicNode(model) {
        override val id = "!head_${model.step.id}"
        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.head, stepConfig.stringAttributeMap(mapOf(
                "id" to id,
                "name" to model.step.name)))
            connections(builder)
            builder.addEndElement()
        }
    }

    open inner class FootNode(model: Model): AtomicNode(model) {
        override val id = "!foot_${model.step.id}"
        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.foot, stepConfig.stringAttributeMap(mapOf(
                "id" to id,
                "name" to model.step.name)))
            connections(builder)
            builder.addEndElement()
        }
    }

    open inner class AtomicStepNode(model: AtomicModel): AtomicNode(model) {
    }

    inner class SubpipelineNode(model: SubpipelineModel): AtomicNode(model) {
        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.subpipeline, stepConfig.stringAttributeMap(mapOf(
                "id" to id,
                "tag" to model.step.instructionType.toString(),
                "name" to model.step.name)))
            connections(builder)
            builder.addEndElement()
        }
    }

    open inner class CompoundNode(model: CompoundModel): ModelNode(model) {
        val head = HeadNode(model.head)
        val foot = FootNode(model.foot)
        init {
            addNode(head)
            addNode(foot)
        }

        override fun describe(builder: SaxonTreeBuilder) {
            val name = if (model is PipelineModel) {
                NsDescription.pipeline
            } else {
                NsDescription.compound
            }
            builder.addStartElement(name, stepConfig.stringAttributeMap(mapOf(
                "object" to this.toString(),
                "id" to id,
                "tag" to model.step.instructionType.toString(),
                "name" to model.step.name)))
            connections(builder)
            head.describe(builder)
            for (child in children) {
                child.describe(builder)
            }
            foot.describe(builder)
            builder.addEndElement()
        }
    }

    inner class InputNode(val port: String): Node() {
        override val id = "I_${port}"
        override fun addSinks() {
            // nop
        }

        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.input, stepConfig.stringAttributeMap(mapOf("port" to port)))
            builder.addEndElement()
        }

        override fun toString(): String {
            return id
        }
    }

    inner class OutputNode(val port: String): Node() {
        override val id = "O_${port}"
        override fun addSinks() {
            // nop
        }

        override fun describe(builder: SaxonTreeBuilder) {
            builder.addStartElement(NsDescription.output, stepConfig.stringAttributeMap(mapOf("port" to port)))
            builder.addEndElement()
        }

        override fun toString(): String {
            return id
        }
    }

    private data class Port(val node: Node, val port: String) {
        override fun toString(): String {
            return "${node.id}/${port}"
        }
    }

    private data class Edge(val from: Port, val to: Port) {
        override fun toString(): String {
            return "${from}->${to}"
        }
    }
}