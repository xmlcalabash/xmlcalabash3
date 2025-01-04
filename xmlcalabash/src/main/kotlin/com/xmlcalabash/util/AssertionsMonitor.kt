package com.xmlcalabash.util

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.LibraryInstruction
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.datamodel.StepDeclaration
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentConverter
import com.xmlcalabash.runtime.PipelineReceiverProxy
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.*
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.value.StringValue
import org.apache.logging.log4j.kotlin.logger

class AssertionsMonitor(): Monitor {
    companion object {
        internal fun parseFromPipeinfo(step: XProcInstruction) {
            for (pipeinfo in step.pipeinfo) {
                if (step is DeclareStepInstruction) {
                    when (step.parent) {
                        is DeclareStepInstruction -> {
                            step.assertions.putAll((step.parent as DeclareStepInstruction).assertions)
                        }
                        is LibraryInstruction -> {
                            step.assertions.putAll((step.parent as LibraryInstruction).assertions)
                        }
                    }
                }

                val assertions = when (step) {
                    is DeclareStepInstruction -> step.assertions
                    is LibraryInstruction -> step.assertions
                    else -> throw XProcError.xiImpossible("Attempting to find assertions in ${step}").exception()
                }

                val info = S9Api.documentElement(pipeinfo)
                for (child in info.axisIterator(Axis.CHILD)) {
                    if (child.nodeKind == XdmNodeKind.ELEMENT) {
                        if (child.nodeName != NsS.schema && child.nodeName != NsP.declareStep) {
                            continue
                        }

                        if (child.getAttributeValue(NsXml.id) == null) {
                            step.stepConfig.warn { "Ignoring ${child.nodeName} assertion without xml:id" }
                            continue
                        }
                        val id = child.getAttributeValue(NsXml.id)!!

                        if (assertions.containsKey(id)) {
                            step.stepConfig.warn { "Error: duplicate xml:id in assertions" }
                        } else {
                            assertions.put(id, child)
                        }
                    }
                }
            }
        }

        internal fun findSchemas(step: XProcInstruction): Map<String, XdmNode> {
            var schemaSource: XProcInstruction? = step
            while (schemaSource != null) {
                when (schemaSource) {
                    is DeclareStepInstruction -> {
                        return schemaSource.assertions
                    }
                    is LibraryInstruction -> {
                        return schemaSource.assertions
                    }
                }
                schemaSource = schemaSource.parent
            }
            return emptyMap()
        }

        internal fun parseStepAssertions(step: StepDeclaration) {
            val assertionsMap = findSchemas(step)
            if (step.extensionAttributes.containsKey(NsCx.assertions)) {
                val assertions = getAssertions(step, step.extensionAttributes[NsCx.assertions]!!)
                for ((port, ids) in assertions) {
                    val portBinding = step.namedInput(port) ?: step.namedOutput(port)
                    if (portBinding == null) {
                        step.stepConfig.warn { "Warning: cx:assertion on non-existant port: $port" }
                    } else {
                        for (id in ids) {
                            val schema = assertionsMap[id]
                            if (schema != null) {
                                portBinding.assertions.add(schema)
                            } else {
                                step.stepConfig.warn { "Warning: cx:assertion references non-existant schema: ${id}"}
                            }
                        }
                    }
                }
            }
        }

        private fun getAssertions(step: StepDeclaration, assertions: String): Map<String,List<String>> {
            val stepConfig = step.stepConfig
            val assertionMap = mutableMapOf<String,MutableList<String>>()
            try {
                val compiler = stepConfig.newXPathCompiler()
                val exec = compiler.compile(assertions)
                val selector = exec.load()
                val value = selector.evaluate()
                if (value !is XdmMap) {
                    step.stepConfig.warn { "The cx:assertions is not a map: ${assertions}" }
                    return emptyMap()
                }

                // Small hack to use qnames, these should just be string keys...
                val map = stepConfig.asMap(stepConfig.forceQNameKeys(value))
                for ((name, values) in map) {
                    if (name.namespaceUri != NamespaceUri.NULL) {
                        step.stepConfig.warn { "The cx:assertions port names must be strings: ${assertions}" }
                        return emptyMap()
                    }
                    val port = name.localName
                    assertionMap[port] = mutableListOf()
                    for (index in 0 ..< values.size()) {
                        val value = values.elementAt(index)
                        if (value.underlyingValue is StringValue) {
                            assertionMap[port]!!.add(value.stringValue)
                        } else {
                            step.stepConfig.warn { "The cx:assertions values must be strings: ${assertions}" }
                            return emptyMap()
                        }
                    }
                }

                return assertionMap
            } catch (ex: Exception) {
                step.stepConfig.warn { "Failed to parse cx:assertion: ${assertions}" }
                step.stepConfig.warn { "  ${ex.message ?: "No explanation"}" }
                return emptyMap()
            }
        }
    }

    override fun startStep(step: AbstractStep) {
        // nop
    }

    override fun endStep(step: AbstractStep) {
        // nop
    }

    override fun abortStep(step: AbstractStep, ex: Exception) {
        // nop
    }

    override fun sendDocument(
        from: Pair<AbstractStep, String>,
        to: Pair<Consumer, String>,
        document: XProcDocument
    ): XProcDocument {
        val outputSchemas = if (from.first is CompoundStepFoot) {
            // Inputs and outputs are "reversed" on a compound step foot.
            from.first.params.inputs[from.second]?.assertions ?: emptyList()
        } else {
            from.first.params.outputs[from.second]?.assertions ?: emptyList()
        }

        val fromType = when (from.first) {
            is CompoundStepHead -> (from.first as CompoundStepHead).parent.type
            is CompoundStepFoot -> (from.first as CompoundStepFoot).parent.type
            else -> from.first.type
        }

        val fromName = if (from.first.name.startsWith("!")) {
            ""
        } else {
            "[${from.first.name}]"
        }

        val fromPort = if (from.second.startsWith("!")) {
            ""
        } else {
            "/${from.second}"
        }

        if (outputSchemas.isNotEmpty()) {
            val stepid = "on ${fromType}${fromName}${fromPort}"
            for (schema in outputSchemas) {
                from.first.stepConfig.debug { "Assertion ${schema.nodeName} ${stepid}"}
                assertion(from.first.stepConfig, stepid, schema, document)
            }
        }

        val toStep: AbstractStep = when (to.first) {
            is AtomicStep -> to.first as AbstractStep
            is CompoundStepHead -> to.first as AbstractStep
            is CompoundStepFoot -> {
                // Handled on the output side...
                return document
            }
            is PipelineReceiverProxy -> {
                return document
            }
            else -> {
                logger.warn { "Unexpected AssertionsMonitor target: ${to.first}" }
                return document
            }
        }

        val toType = when (toStep) {
            is CompoundStepHead -> toStep.parent.type
            is CompoundStepFoot -> toStep.parent.type
            else -> toStep.type
        }

        val toName = if (toStep.name.startsWith("!")) {
            ""
        } else {
            "[${toStep.name}]"
        }

        val toPort = if (to.second.startsWith("!")) {
            ""
        } else {
            "/${to.second}"
        }

        val inputSchemas =  toStep.params.inputs[to.second]?.assertions ?: emptyList()

        if (inputSchemas.isNotEmpty()) {
            val stepid = "on ${toType}${toName}${toPort}"
            for (schema in inputSchemas) {
                toStep.stepConfig.debug { "Assertion ${schema.nodeName} ${stepid}"}
                assertion(toStep.stepConfig, stepid, schema, document)
            }
        }

        return document
    }

    private fun assertion(stepConfig: XProcStepConfiguration, stepid: String, schema: XdmNode, document: XProcDocument) {
        when (schema.nodeName) {
            NsS.schema -> schematron(stepConfig, stepid, schema, document)
            NsP.declareStep -> pipeline(stepConfig, stepid, schema, document)
            else -> stepConfig.warn { "Unexpected assertion type ${schema.nodeName} ${stepid}" }
        }
    }

    private fun schematron(stepConfig: XProcStepConfiguration, stepid: String, schema: XdmNode, document: XProcDocument) {
        val validator = SchematronImpl(stepConfig)
        if (document is XProcBinaryDocument) {
            stepConfig.info({ "Schematron assertions cannot be applied to binary documents" })
            return
        }

        val testDocument = if (document.value is XdmNode) {
            document.value
        } else {
            try {
                DocumentConverter(stepConfig, document, MediaType.XML).convert().value as XdmNode
            } catch (ex: Exception) {
                stepConfig.warn({ "Failed to convert non-XML input to XML for Schematron testing: ${document.contentType}: ${ex.message}" })
                return
            }
        }

        val results = validator.test(testDocument, schema)

        if (results.isNotEmpty()) {
            val level = stepConfig.environment.assertions
            for (result in results) {
                val text = getText(result)
                when (result.nodeName) {
                    NsSvrl.successfulReport -> {
                        stepConfig.info { "Report ${stepid}: ${text}" }
                    }
                    NsSvrl.failedAssert -> {
                        if (level == AssertionsLevel.WARNING) {
                            stepConfig.warn { "Assert ${stepid}: ${text}" }
                        } else {
                            stepConfig.error { "Assert ${stepid}: ${text}" }
                            throw XProcError.xiAssertionFailed(text).exception()
                        }
                    }
                    else -> {
                        stepConfig.warn { "Unexpected assertion result: ${result.nodeName}" }
                    }
                }
            }
        }
    }

    private fun pipeline(stepConfig: XProcStepConfiguration, stepid: String, schema: XdmNode, document: XProcDocument) {
        val docBuilder = SaxonTreeBuilder(schema.processor)
        docBuilder.startDocument(schema.baseURI)
        docBuilder.addSubtree(schema)
        docBuilder.endDocument()

        val builder = stepConfig.xmlCalabash.newPipelineBuilder(3.0) // FIXME: track real versions?
        val parser = stepConfig.xmlCalabash.newXProcParser(builder)
        val declStep = parser.parse(docBuilder.result)
        if (declStep.inputs().size != 1) {
            stepConfig.warn { "Cannot use pipeline for assertion, pipeline has ${declStep.inputs().size} input ports" }
            return
        }
        if (declStep.getInput("source") == null) {
            stepConfig.warn { "Cannot use pipeline for assertion, pipeline must have a 'source' input port" }
            return
        }
        val pipeline = declStep.runtime().executable()
        pipeline.receiver = DiscardingReceiver()
        pipeline.input("source", document)
        try {
            pipeline.run()
        } catch (ex: Exception) {
            val userMessage = detailMessage(ex)
            val code = if (ex is XProcException) { "${ex.error.code} " } else { "" }
            val level = stepConfig.environment.assertions
            if (level == AssertionsLevel.WARNING) {
                stepConfig.warn { "Assert ${code}${stepid}${userMessage}" }
            } else {
                stepConfig.error { "Assert ${code}${stepid}${userMessage}" }
                throw XProcError.xiAssertionFailed(code, userMessage).exception(ex)
            }
        }
    }

    private fun detailMessage(ex: Exception): String {
        if (ex is XProcException) {
            if (ex.error.details.isNotEmpty() && ex.error.details.first() is XProcDocument) {
                val value = (ex.error.details.first() as XProcDocument).value
                val text = value.underlyingValue.stringValue
                if (text.isNotEmpty()) {
                    return ": ${text}"
                }
            }
        }
        return ""
    }

    private fun getText(node: XdmNode): String {
        for (child in node.axisIterator(Axis.CHILD)) {
            if (child.nodeKind == XdmNodeKind.ELEMENT && child.nodeName == NsSvrl.text) {
                return child.stringValue
            }
        }
        return "??? no svrl:text in result"
    }
}