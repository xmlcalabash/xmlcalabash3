package com.xmlcalabash.util

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.LibraryInstruction
import com.xmlcalabash.datamodel.StepDeclaration
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsS
import com.xmlcalabash.namespace.NsSvrl
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.runtime.Monitor
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.*
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.value.StringValue
import org.apache.logging.log4j.kotlin.logger

class SchematronMonitor(): Monitor {
    companion object {
        internal fun parseFromPipeinfo(step: XProcInstruction) {
            for (pipeinfo in step.pipeinfo) {
                if (step is DeclareStepInstruction) {
                    when (step.parent) {
                        is DeclareStepInstruction -> {
                            step.schematron.putAll((step.parent as DeclareStepInstruction).schematron)
                        }
                        is LibraryInstruction -> {
                            step.schematron.putAll((step.parent as LibraryInstruction).schematron)
                        }
                    }
                }

                val schematron = when (step) {
                    is DeclareStepInstruction -> step.schematron
                    is LibraryInstruction -> step.schematron
                    else -> throw XProcError.xiImpossible("Attempting to find schematron assertions in ${step}").exception()
                }

                val info = S9Api.documentElement(pipeinfo)
                for (child in info.axisIterator(Axis.CHILD)) {
                    if (child.nodeKind == XdmNodeKind.ELEMENT && child.nodeName == NsS.schema) {
                        if (child.getAttributeValue(NsXml.id) == null) {
                            logger.warn { "Ignoring schematron schema without xml:id"}
                        } else {
                            val id = child.getAttributeValue(NsXml.id)!!
                            if (schematron.containsKey(id)) {
                                logger.warn { "Error: duplicate xml:id in schematron schemas"}
                            } else {
                                schematron.put(id, child)
                            }
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
                        return schemaSource.schematron
                    }
                    is LibraryInstruction -> {
                        return schemaSource.schematron
                    }
                }
                schemaSource = schemaSource.parent
            }
            return emptyMap()
        }

        internal fun parseStepAssertions(step: StepDeclaration) {
            val schematron = SchematronMonitor.findSchemas(step)
            if (schematron.isNotEmpty()) {
                if (step.extensionAttributes.containsKey(NsCx.assertions)) {
                    val assertions = getAssertions(step, step.extensionAttributes[NsCx.assertions]!!)
                    for ((port, ids) in assertions) {
                        val portBinding = step.namedInput(port) ?: step.namedOutput(port)
                        if (portBinding == null) {
                            logger.warn { "Error: cx:assertion on non-existant port: $port" }
                        } else {
                            for (id in ids) {
                                val schema = schematron[id]
                                if (schema != null) {
                                    portBinding.schematron.add(schema)
                                } else {
                                    logger.warn { "Error: cx:assertion references non-existint schema: ${id}"}
                                }
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
                    logger.warn { "The cx:assertions is not a map: ${assertions}" }
                    return emptyMap()
                }

                // Small hack to use qnames, these should just be string keys...
                val map = stepConfig.asMap(stepConfig.forceQNameKeys(value))
                for ((name, values) in map) {
                    if (name.namespaceUri != NamespaceUri.NULL) {
                        logger.warn { "The cx:assertions port names must be strings: ${assertions}" }
                        return emptyMap()
                    }
                    val port = name.localName
                    assertionMap[port] = mutableListOf()
                    for (index in 0 ..< values.size()) {
                        val value = values.elementAt(index)
                        if (value.underlyingValue is StringValue) {
                            assertionMap[port]!!.add(value.stringValue)
                        } else {
                            logger.warn { "The cx:assertions values must be strings: ${assertions}" }
                            return emptyMap()
                        }
                    }
                }

                return assertionMap
            } catch (ex: Exception) {
                logger.warn { "Failed to parse cx:assertion: ${assertions}" }
                logger.warn { "  ${ex.message ?: "No explanation"}" }
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
            from.first.params.inputs[from.second]?.schematron ?: emptyList()
        } else {
            from.first.params.outputs[from.second]?.schematron ?: emptyList()
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
            logger.debug { "Schematron ${stepid}"}

            for (schema in outputSchemas) {
                schematron(from.first.stepConfig, stepid, schema, document)
            }
        }

        val toStep: AbstractStep = when (to.first) {
            is AtomicStep -> to.first as AbstractStep
            is CompoundStepHead -> to.first as AbstractStep
            is CompoundStepFoot -> to.first as AbstractStep
            else -> {
                logger.debug { "Unexpected SchematronMonitor target: ${to.first}" }
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

        val inputSchemas =  toStep.params.inputs[to.second]?.schematron ?: emptyList()

        if (inputSchemas.isNotEmpty()) {
            val stepid = "on ${toType}${toName}${toPort}"
            logger.debug { "Schematron ${stepid}" }
            for (schema in inputSchemas) {
                schematron(toStep.stepConfig, stepid, schema, document)
            }
        }

        return document
    }

    private fun schematron(stepConfig: XProcStepConfiguration, stepid: String, schema: XdmNode, document: XProcDocument) {
        val validator = SchematronImpl(stepConfig)
        if (document.value is XdmNode) {
            val results = validator.test(document.value as XdmNode, schema)
            if (results.isNotEmpty()) {
                val messages = stepConfig.environment.messageReporter
                val level = stepConfig.xmlCalabash.xmlCalabashConfig.assertions
                for (result in results) {
                    val location = result.getAttributeValue(Ns.location) ?: ""
                    val test = result.getAttributeValue(Ns.test) ?: ""
                    val text = getText(result)
                    when (result.nodeName) {
                        NsSvrl.successfulReport -> {
                            if (document.baseURI != null) {
                                messages.info { "At ${location} ${stepid} in ${document.baseURI}:" }
                            } else {
                                messages.info { "At ${location} ${stepid}:" }
                            }
                            messages.info { "  Report ${test}: ${text}"}
                        }
                        NsSvrl.failedAssert -> {
                            if (level == SchematronAssertions.WARNING) {
                                if (document.baseURI != null) {
                                    messages.warn { "At ${location} ${stepid} in ${document.baseURI}:" }
                                } else {
                                    messages.warn { "At ${location} ${stepid}:" }
                                }
                                messages.warn { "  Warning ${test}: ${text}" }
                            } else {
                                if (document.baseURI != null) {
                                    messages.error { "At ${location} ${stepid} in ${document.baseURI}:" }
                                } else {
                                    messages.error { "At ${location} ${stepid}:" }
                                }
                                messages.error { " Error ${test} : ${text}" }
                                throw XProcError.xiAssertionFailed("${test} : ${text}").exception()
                            }
                        }
                        else -> {
                            logger.warn { "Unexpected assertion result: ${result.nodeName}" }
                        }
                    }
                }
            }
        } else {
            logger.info { "Schematron assertions on ports only apply to nodes" }
        }
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