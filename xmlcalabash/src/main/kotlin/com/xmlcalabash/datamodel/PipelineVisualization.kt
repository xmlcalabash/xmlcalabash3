package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class PipelineVisualization private constructor(val instruction: XProcInstruction, private val filenameMap: Map<String,String>) {
    companion object {
        private var _id = 0

        fun build(pipeline: DeclareStepInstruction, filenameMap: Map<String,String>): XdmNode {
            val vis = PipelineVisualization(pipeline, filenameMap)
            val xml = vis.build()
            return xml
        }
    }

    private var root: Compound? = null
    private var parent: Compound? = null
    private val readableMap = mutableMapOf<PortBindingContainer,Int>()
    private val writeableMap = mutableMapOf<PortBindingContainer,Int>()
    private val pipeToPort = mutableMapOf<PipeInstruction,PortBindingContainer>()
    private val compoundDepends = mutableMapOf<CompoundStepDeclaration, Int>()
    private val allPrefixes = mutableSetOf<String>()
    private var gPrefix = "g"

    private fun build(): XdmNode {
        buildPipeMap(instruction)

        while (allPrefixes.contains(gPrefix)) {
            gPrefix += "_"
        }

        var nsmap: NamespaceMap = NamespaceMap.emptyMap()
        nsmap = nsmap.put(gPrefix, NsDescription.namespace)
        nsmap = nsmap.put("p", NsP.namespace)
        nsmap = nsmap.put("cx", NsCx.namespace)
        nsmap = nsmap.put("xs", NsXs.namespace)
        describe(instruction, nsmap)

        val builder = SaxonTreeBuilder(instruction.stepConfig)
        builder.startDocument(null)
        root!!.makeXml(builder)
        builder.endDocument()
        val xml = builder.result

        val readFrom = mutableSetOf<PortBindingContainer>()
        readFrom.addAll(readableMap.keys.filter { it !is OutputInstruction || it.parent !is DeclareStepInstruction })
        for ((_, readable) in pipeToPort) {
            readFrom.remove(readable)
        }

        return xml
    }

    private fun describe(instruction: XProcInstruction, nsmap: NamespaceMap) {
        when (instruction) {
            is DeclareStepInstruction -> declareStep(instruction, nsmap)
            is CompoundStepDeclaration -> compoundStep(instruction, nsmap)
            is StepDeclaration -> atomicStep(instruction, nsmap)
            else -> {
                instruction.stepConfig.error { "Unexpected instruction type in pipeline visualization: ${instruction.instructionType}" }
            }
        }
    }

    private fun declareStep(pipeline: DeclareStepInstruction, nsmap: NamespaceMap) {
        var localns = nsmap
        for ((prefix, uri) in pipeline.inscopeNamespaces) {
            localns = localns.put(prefix, uri)
        }

        val compound = Compound(localns, pipeline.name, pipeline.instructionType,
            mapOf(
                "base-uri" to pipeline.stepConfig.baseUri?.toString(),
                "name" to pipeline.name,
                "id" to pipeline.id.toString(),
                "filename" to filenameMap[pipeline.id],
                "type" to pipeline.type?.toString(),
                "version" to pipeline.version?.toString(),
                "xpath-version" to pipeline.xpathVersion?.toString(),
                "psvi-required" to pipeline.psviRequired?.toString()
            ))

        val head = Head(localns)
        val foot = Foot(localns)

        for (input in pipeline.children.filterIsInstance<InputInstruction>()) {
            val iport = head.addInput(input)
            writeableMap[input] = iport.id
            val oport = head.addOutput(input)
            readableMap[input] = oport.id
        }

        for (output in pipeline.children.filterIsInstance<OutputInstruction>()) {
            val iport = foot.addInput(output)
            writeableMap[output] = iport.id
            val oport = foot.addOutput(output)
            readableMap[output] = oport.id
        }

        if (root == null) {
            root = compound
        }
        parent = compound

        compound.addChild(head)

        for (child in pipeline.children) {
            when (child) {
                is InputInstruction, is OutputInstruction -> Unit
                else -> describe(child, localns)
            }
        }

        compound.addChild(foot)

        addPipes(compound, pipeline)
    }

    private fun compoundStep(step: CompoundStepDeclaration, nsmap: NamespaceMap) {
        var localns = nsmap
        for ((prefix, uri) in step.inscopeNamespaces) {
            localns = localns.put(prefix, uri)
        }

        val compound = Compound(localns, step.name, step.instructionType, emptyMap())

        parent!!.addChild(compound)

        val head = Head(localns)
        val foot = Foot(localns)

        for (input in step.children.filterIsInstance<WithInputInstruction>()) {
            val iport = head.addInput(input)
            writeableMap[input] = iport.id
        }

        for (input in step.children.filterIsInstance<InputInstruction>()) {
            val iport = head.addOutput(input)
            readableMap[input] = iport.id
        }

        for (output in step.children.filterIsInstance<OutputInstruction>()) {
            val iport = foot.addInput(output)
            writeableMap[output] = iport.id
            val oport = foot.addOutput(output)
            readableMap[output] = oport.id
        }

        if (compoundDepends.contains(step)) {
            val port = NamedPort(localns, "!depends", primary=false, sequence=true)
            foot.outputs.add(port)
            compoundDepends[step] = port.id
        }

        var saveParent = parent
        parent = compound

        compound.addChild(head)
        for (child in step.children) {
            when (child) {
                is InputInstruction, is OutputInstruction -> Unit
                is WithInputInstruction, is WithOutputInstruction -> Unit
                else -> describe(child, localns)
            }
        }
        compound.addChild(foot)

        addPipes(compound, step)

        parent = saveParent
    }

    private fun atomicStep(step: StepDeclaration, nsmap: NamespaceMap) {
        var localns = nsmap
        for ((prefix, uri) in step.inscopeNamespaces) {
            localns = localns.put(prefix, uri)
        }

        val attr = mutableMapOf<String,String?>()

        attr["filename"] = filenameMap[step.declId]

        if (step is AtomicDocumentStepInstruction && step.staticOptions.containsKey(Ns.href)) {
            val opt = step.staticOptions[Ns.href]!!
            attr["href"] = opt.staticValue.toString()
        }

        if (step is AtomicSelectStepInstruction) {
            attr["select"] = step.select.toString()
        }

        if (step is AtomicExpressionStepInstruction) {
            attr["as"] = step.expression.asType.underlyingSequenceType.toString()
            // It's kind of a bug that the test expression for a when has a binding name...
            if (step.bindingName != Ns.test || step.parent !is WhenInstruction) {
                if (step.externalName != null) {
                    attr["option-name"] = step.externalName?.toString()
                } else {
                    attr["variable-name"] = "${step.bindingName}"
                }
            }
            attr["expression"] = step.expression.toString()
            if (step.expression.values.isNotEmpty()) {
                attr["values"] = step.expression.values.toString()
            }
        }

        val atomic = Atomic(localns, step.name, step.instructionType, attr)

        parent!!.addChild(atomic)

        for (input in step.children.filterIsInstance<WithInputInstruction>()) {
            val port = atomic.addInput(input)
            writeableMap[input] = port.id
        }

        for (output in step.children.filterIsInstance<WithOutputInstruction>()) {
            val port = atomic.addOutput(output)
            readableMap[output] = port.id
        }
    }

    private fun addPipes(compound: Compound, node: XProcInstruction) {
        for (child in node.children) {
            when (child) {
                is CompoundStepDeclaration -> Unit
                is PipeInstruction -> {
                    val to = writeableMap[node as PortBindingContainer]!!
                    val fromPort = pipeToPort[child]
                    if (fromPort == null) {
                        val depends = findCompoundDepends(child, node)
                        val from = compoundDepends[depends]!!
                        compound.edges.add(Edge(from, to, compound.name, "!depends", child.step!!, child.port!!, child.implicit))
                    } else {
                        val from = readableMap[fromPort]!!
                        var step: XProcInstruction = child
                        while (step !is StepDeclaration) {
                            step = step.parent!!
                        }
                        compound.edges.add(Edge(from, to, step.name, fromPort.port, child.step!!, child.port!!, child.implicit))
                    }
                }
                else -> addPipes(compound, child)
            }
        }
    }

    private fun buildPipeMap(instruction: XProcInstruction) {
        for ((prefix, _) in instruction.inscopeNamespaces) {
            allPrefixes.add(prefix)
        }
        for (child in instruction.children) {
            if (child is PipeInstruction) {
                findPort(child, instruction)
            } else {
                buildPipeMap(child)
            }
        }
    }

    private fun findPort(pipe: PipeInstruction, node: XProcInstruction) {
        if (node !is CompoundStepDeclaration) {
            findPort(pipe, node.parent!!)
            return
        }

        if (node.name == pipe.step) {
            for (input in node.children.filterIsInstance<InputInstruction>()) {
                if (input.port == pipe.port) {
                    pipeToPort[pipe] = input
                    return
                }
            }
            throw XProcError.xiImpossible("Did not find readable input ${pipe.step}/${pipe.port}").exception()
        }

        for (child in node.children.filterIsInstance<StepDeclaration>()) {
            if (child.name == pipe.step) {
                if (child is CompoundStepDeclaration) {
                    for (output in child.children.filterIsInstance<OutputInstruction>()) {
                        if (output.port == pipe.port) {
                            pipeToPort[pipe] = output
                            return
                        }
                    }
                } else {
                    for (output in child.children.filterIsInstance<WithOutputInstruction>()) {
                        if (output.port == pipe.port) {
                            pipeToPort[pipe] = output
                            return
                        }
                    }
                }

                if (pipe.port == "!depends" && child is CompoundStepDeclaration) {
                    // Weird special case
                    compoundDepends[child] = -1
                    return
                }

                throw XProcError.xiImpossible("Did not find readable port ${pipe.step}/${pipe.port}").exception()
            }
        }

        return findPort(pipe, node.parent!!)
    }

    private fun findCompoundDepends(pipe: PipeInstruction, node: XProcInstruction): CompoundStepDeclaration {
        if (node !is CompoundStepDeclaration) {
            return findCompoundDepends(pipe, node.parent!!)
        }

        if (node.name == pipe.step) {
            return node
        }

        for (child in node.children.filterIsInstance<StepDeclaration>()) {
            if (child.name == pipe.step) {
                if (child is CompoundStepDeclaration) {
                    return child
                }

                throw XProcError.xiImpossible("Did not find readable port ${pipe.step}/${pipe.port}").exception()
            }
        }

        return findCompoundDepends(pipe, node.parent!!)
    }

    private inner class Edge(val from: Int, val to: Int, val fromStep: String, val fromPort: String, val toStep: String, val toPort: String, val implicit: Boolean)

    private inner class NamedPort(val nsmap: NamespaceMap, val name: String, val primary: Boolean, val sequence: Boolean) {
        var weldedShut = false
        val id = ++_id

        fun makeXml(builder: SaxonTreeBuilder) {
            val attr = mutableMapOf<String,String?>(
                "id" to "_${id}",
                "primary" to "${primary}",
                "sequence" to "${sequence}",
                "welded-shut" to (if (weldedShut) "true" else null)
            )
            builder.addStartElement(NsDescription.g("port", gPrefix), instruction.stepConfig.stringAttributeMap(attr), nsmap)
            builder.addText(name)
            builder.addEndElement()
        }

        override fun toString(): String {
            return "${name} / ${id}"
        }
    }

    private abstract inner class Step(val nsmap: NamespaceMap, val name: String, val type: QName, attributes: Map<String,String?>) {
        val id = ++_id
        val attributes = mutableMapOf<String,String?>()

        init {
            this.attributes.putAll(attributes)
        }

        override fun toString(): String {
            return "${type} / ${name} / ${id}"
        }

        abstract fun makeXml(builder: SaxonTreeBuilder)
    }

    private open inner class Atomic(nsmap: NamespaceMap, name: String, type: QName, attributes: Map<String,String?> = emptyMap()): Step(nsmap, name, type, attributes) {
        val inputs = mutableListOf<NamedPort>()
        val outputs = mutableListOf<NamedPort>()

        fun addInput(input: PortBindingContainer): NamedPort {
            val port = NamedPort(nsmap, input.port, input.primary == true, input.sequence == true)
            port.weldedShut = input.weldedShut
            inputs.add(port)
            return port
        }

        fun addOutput(output: PortBindingContainer): NamedPort {
            val port = NamedPort(nsmap, output.port, output.primary == true, output.sequence == true)
            port.weldedShut = output.weldedShut
            outputs.add(port)
            return port
        }

        override fun makeXml(builder: SaxonTreeBuilder) {
            val attr = mutableMapOf<String,String?>()

            val gi = when (type) {
                NsCx.head -> NsDescription.g("head", gPrefix)
                NsCx.foot -> NsDescription.g("foot", gPrefix)
                else -> {
                    attr["name"] = name
                    attr["type"] = "${type}"
                    NsDescription.g("atomic-step", gPrefix)
                }
            }

            attr.putAll(attributes)

            builder.addStartElement(gi, instruction.stepConfig.stringAttributeMap(attr), nsmap)
            builder.addStartElement(NsDescription.g("inputs", gPrefix), EmptyAttributeMap.getInstance(), nsmap)
            for (input in inputs) {
                input.makeXml(builder)
            }
            builder.addEndElement()
            builder.addStartElement(NsDescription.g("outputs", gPrefix), EmptyAttributeMap.getInstance(), nsmap)
            for (output in outputs) {
                output.makeXml(builder)
            }
            builder.addEndElement()
            builder.addEndElement()
        }
    }

    private inner class Head(nsmap: NamespaceMap): Atomic(nsmap, "anonymous", NsCx.head, emptyMap())
    private inner class Foot(nsmap: NamespaceMap): Atomic(nsmap, "anonymous", NsCx.foot, emptyMap())

    private open inner class Compound(nsmap: NamespaceMap, name: String, type: QName, attributes: Map<String,String?> = emptyMap()): Step(nsmap, name, type, attributes) {
        val children = mutableListOf<Step>()
        val edges = mutableListOf<Edge>()

        fun addChild(step: Step) {
            children.add(step)
        }

        override fun makeXml(builder: SaxonTreeBuilder) {
            val attr = mutableMapOf<String,String?>(
                "name" to name,
                "type" to "${type}"
            )
            attr.putAll(attributes)

            if (type == NsP.declareStep) {
                builder.addStartElement(NsDescription.g("declare-step", gPrefix), instruction.stepConfig.stringAttributeMap(attr), nsmap)
                val head = children.filterIsInstance<Head>().first()
                io(builder, head.inputs, NsDescription.g("input", gPrefix))
                val foot = children.filterIsInstance<Foot>().first()
                io(builder, foot.outputs, NsDescription.g("output", gPrefix))
            } else {
                builder.addStartElement(NsDescription.g("compound-step", gPrefix), instruction.stepConfig.stringAttributeMap(attr), nsmap)
            }
            for (child in children) {
                child.makeXml(builder)
            }

            for (edge in edges) {
                var attr = mapOf(
                    "from" to "_${edge.from}",
                    "to" to "_${edge.to}",
                    "from-step" to edge.fromStep,
                    "from-port" to edge.fromPort,
                    "to-step" to edge.toStep,
                    "to-port" to edge.toPort,
                    "implicit" to (if (edge.implicit) "true" else null),
                    )
                builder.addStartElement(NsDescription.g("edge", gPrefix), instruction.stepConfig.stringAttributeMap(attr), nsmap)
                builder.addEndElement()
            }

            if (type == NsP.declareStep) {
                val head = children.filterIsInstance<Head>().first()
                for (port in head.inputs) {
                    val edgeattr = mapOf(
                        "from" to "input_${port.id}",
                        "to" to "_${port.id}"
                    )
                    builder.addStartElement(NsDescription.g("edge", gPrefix), instruction.stepConfig.stringAttributeMap(edgeattr), nsmap)
                    builder.addEndElement()
                }

                val foot = children.filterIsInstance<Foot>().first()
                for (port in foot.outputs) {
                    val edgeattr = mapOf(
                        "from" to "_${port.id}",
                        "to" to "output_${port.id}"
                    )
                    builder.addStartElement(NsDescription.g("edge", gPrefix), instruction.stepConfig.stringAttributeMap(edgeattr), nsmap)
                    builder.addEndElement()
                }
            }

            builder.addEndElement()
        }

        private fun io(builder: SaxonTreeBuilder, ports: List<NamedPort>, element: QName) {
            val prefix = if (element == NsDescription.g("input", gPrefix)) { "input_" } else { "output_" }
            for (port in ports) {
                val attr = mapOf(
                    "id" to "${prefix}${port.id}",
                    "primary" to "${port.primary}",
                    "sequence" to "${port.sequence}"
                )
                builder.addStartElement(element, EmptyAttributeMap.getInstance(), nsmap)
                builder.addStartElement(NsDescription.g("port", gPrefix), instruction.stepConfig.stringAttributeMap(attr), nsmap)
                builder.addText(port.name)
                builder.addEndElement()
                builder.addEndElement()
            }
        }
    }
}