package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class PipelineVisualization private constructor(private val instruction: XProcInstruction) {
    companion object {
        fun build(pipeline: DeclareStepInstruction): XdmNode {
            val vis = PipelineVisualization(pipeline)
            val xml = vis.build()
            return xml
        }

        fun build(library: LibraryInstruction): XdmNode {
            val vis = PipelineVisualization(library)
            val xml = vis.build()
            return xml
        }

        private val ns = NamespaceUri.of("http://xmlcalabash.com/ns/description")
        private val ns_compound_step = QName(ns, "compound-step")
        private val ns_atomic_step = QName(ns, "atomic-step")
    }

    lateinit var builder: SaxonTreeBuilder
    var root = true

    private fun build(): XdmNode {
        builder = SaxonTreeBuilder(instruction.stepConfig)
        builder.startDocument(null)
        describe(instruction)
        builder.endDocument()
        return builder.result
    }

    private fun describe(instruction: XProcInstruction) {
        when (instruction) {
            is DeclareStepInstruction -> declareStep(instruction)
            is LibraryInstruction -> library(instruction)
            is AtomicExpressionStepInstruction -> expressionStep(instruction)
            is StepDeclaration -> step(instruction)
            is PortBindingContainer -> portBindingContainer(instruction)
            is VariableBindingContainer -> variableBindingContainer(instruction)
            is PipeInstruction -> pipe(instruction)
            else -> {
                startElement(instruction.instructionType, mapOf(
                    "ERROR" to "ERROR"
                ))
                for (child in instruction.children) {
                    describe(child)
                }
                builder.addEndElement()
            }
        }
    }

    private fun startElement(qname: QName, attr: Map<String,String?>) {
        var nsmap: NamespaceMap = NamespaceMap.emptyMap()
        nsmap = nsmap.put("", ns)
        nsmap = nsmap.put("p", NsP.namespace)
        nsmap = nsmap.put("cx", NsCx.namespace)
        nsmap = nsmap.put("xs", NsXs.namespace)

        val name = if (qname.namespaceUri == ns) {
            qname
        } else {
            QName(ns, qname.localName)
        }

        builder.addStartElement(name, instruction.stepConfig.stringAttributeMap(attr), nsmap)
        root = false
    }

    private fun library(library: LibraryInstruction) {
        startElement(library.instructionType, mapOf(
            "version" to library.version?.toString(),
            "xpath-version" to library.xpathVersion?.toString(),
            "psvi-required" to library.psviRequired?.toString()
        ))
        for (child in library.children) {
            describe(child)
        }
        builder.addEndElement()
    }

    private fun declareStep(pipeline: DeclareStepInstruction) {
        startElement(pipeline.instructionType, mapOf(
            "name" to pipeline.name,
            "id" to pipeline.id.toString(),
            "type" to pipeline.type?.toString(),
            "version" to pipeline.version?.toString(),
            "xpath-version" to pipeline.xpathVersion?.toString(),
            "psvi-required" to pipeline.psviRequired?.toString()
        ))

        for (child in pipeline.children) {
            describe(child)
        }

        builder.addEndElement()
    }

    private fun expressionStep(step: AtomicExpressionStepInstruction) {
        val attr = mutableMapOf<String,String?>(
            "name" to step.name,
            "type" to step.instructionType.toString(),
            "expression" to step.expression.toString(),
            "as" to step.expression.asType.underlyingSequenceType.toString(),
        )

        if (step.expression.values.isNotEmpty()) {
            attr["values"] = step.expression.values.toString()
        }

        startElement(ns_atomic_step, attr)

        for (child in step.children) {
            describe(child)
        }
        builder.addEndElement()
    }

    private fun step(step: StepDeclaration) {
        val attr = mutableMapOf<String,String?>(
            "name" to step.name,
            "type" to step.instructionType.toString(),
        )
        if (step is AtomicDocumentStepInstruction && step.staticOptions.containsKey(Ns.href)) {
            attr["href"] = step.staticOptions[Ns.href].toString()
        }
        if (step is AtomicSelectStepInstruction) {
            attr["select"] = step.select.toString()
        }

        if (step is CompoundStepDeclaration) {
            startElement(ns_compound_step, attr)
        } else {
            startElement(ns_atomic_step, attr)
        }

        val excludeOptions = mutableSetOf<QName>()
        for (child in step.children.filterIsInstance<WithInputInstruction>()) {
            if (child.port.startsWith("Q{")) {
                val qname = child.stepConfig.parseQName(child.port)
                excludeOptions.add(qname)
            }
        }

        for (child in step.children) {
            if (child !is WithOptionInstruction || !excludeOptions.contains(child.name)) {
                describe(child)
            }
        }
        builder.addEndElement()
    }

    private fun portBindingContainer(port: PortBindingContainer) {
        val attr = mutableMapOf<String, String?>()
        attr["port"] = port.port
        attr["primary"] = port.primary?.toString()
        attr["sequence"] = port.sequence?.toString()
        if (port.weldedShut) {
            attr["welded-shut"] = port.weldedShut.toString()
        }

        startElement(port.instructionType, attr)
        for (child in port.children) {
            describe(child)
        }
        builder.addEndElement()
    }

    private fun variableBindingContainer(variable: VariableBindingContainer) {
        startElement(variable.instructionType, mapOf(
            "name" to variable.name.toString(),
            "as" to variable.asType?.underlyingSequenceType?.toString(),
            "select" to variable.select?.toString(),
            "collection" to variable.collection?.toString()
        ))
        for (child in variable.children) {
            describe(child)
        }
        builder.addEndElement()
    }

    private fun pipe(pipe: PipeInstruction) {
        startElement(pipe.instructionType, mapOf(
            "step" to pipe.step,
            "port" to pipe.port
        ))
        for (child in pipe.children) {
            describe(child)
        }
        builder.addEndElement()
    }
}