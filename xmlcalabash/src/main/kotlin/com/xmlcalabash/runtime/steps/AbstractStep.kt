package com.xmlcalabash.runtime.steps

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.model.StepModel
import com.xmlcalabash.runtime.parameters.DocumentStepParameters
import com.xmlcalabash.runtime.parameters.InlineStepParameters
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.internal.DocumentStep
import com.xmlcalabash.steps.internal.InlineStep
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

abstract class AbstractStep(val stepConfig: RuntimeStepConfiguration, step: StepModel): Consumer {
    val id: String = step.id
    val name: String = step.name
    val type: QName = step.type
    val location = step.location
    internal val receiver = mutableMapOf<String, Pair<Consumer, String>>()
    val inputCount = mutableMapOf<String, Int>()
    val outputCount = mutableMapOf<String, Int>()
    val staticOptions = step.staticOptions.toMutableMap()
    val verbosity = stepConfig.saxonConfig.xmlCalabash.xmlCalabashConfig.verbosity
    val debug = stepConfig.saxonConfig.xmlCalabash.xmlCalabashConfig.debug

    abstract val params: RuntimeStepParameters
    abstract val readyToRun: Boolean
    abstract fun output(port: String, document: XProcDocument)
    abstract fun instantiate()
    abstract fun run()

    internal fun checkInputPort(port: String, doc: XProcDocument, flange: RuntimePort?): XProcError? {
        if (flange == null) {
            logger.warn { "Unexpected input port: ${port} on ${this}" }
            return null
        }

        val count = (inputCount[port] ?: 0) + 1
        if (!flange.sequence && count != 1) {
            return XProcError.xdInputSequenceForbidden(port)
        }

        if (flange.contentTypes.isNotEmpty()) {
            val mtype = doc.contentType ?: MediaType.ANY
            val match = mtype.matchingMediaType(flange.contentTypes)
            if (match == null || !match.inclusive) {
                return XProcError.xdBadInputContentType(port, mtype.toString())
            }
        }

        inputCount[port] = count
        //println("input ${port}: on ${this}")
        return null
    }

    internal fun checkInputPort(port: String, flange: RuntimePort?): XProcError? {
        if (flange == null) {
            logger.warn { "Unexpected input port: ${port} on ${this}" }
            return null
        }

        val count = inputCount[port] ?: 0
        if (!flange.sequence && count != 1) {
            return XProcError.xdInputSequenceForbidden(port)
        }

        //println("total inputs on ${port}: ${count}")
        return null
    }

    internal fun checkOutputPort(port: String, doc: XProcDocument, flange: RuntimePort?) {
        if (flange == null) {
            logger.warn { "Unexpected output port: ${port} on ${this}" }
            return
        }

        val count = (outputCount[port] ?: 0) + 1
        if (!flange.sequence && count != 1) {
            if (type == NsCx.select) {
                // This is really an error on the input we're feeding into
                throw XProcError.xdInputSequenceForbidden(port).exception()
            }
            throw XProcError.xdOutputSequenceForbidden(port).exception()
        }

        if (flange.contentTypes.isNotEmpty()) {
            val mtype = doc.contentType ?: MediaType.ANY
            val match = mtype.matchingMediaType(flange.contentTypes)
            if (match == null || !match.inclusive) {
                throw XProcError.xdBadOutputContentType(port, mtype.toString()).exception()
            }
        }

        outputCount[port] = count
        //println("output ${port}: on ${this}")
    }

    open fun runStep() {
        logger.debug { "Running ${this}" }
        if (debug || verbosity < Verbosity.NORMAL) {
            println("Running ${this}")
        }

        try {
            run()
        } catch (ex: Exception) {
            when (ex) {
                is XProcException -> {
                    ex.error.at(type, name).at(location)
                    throw ex
                }
                else -> {
                    val msg = ex.message ?: ""
                    if (msg.contains("cannot be cast")) {
                        throw XProcError.xdBadType(msg).at(type,name).exception()
                    }
                    throw XProcError.xdStepFailed(msg).at(type, name).exception()
                }
            }
        }
    }

    open fun reset() {
        inputCount.clear()
        outputCount.clear()
    }

    fun defaultBindingDocuments(binding: ConnectionInstruction): List<XProcDocument> {
        // Reusing the atomic steps to construct the values feels like a tremendous hack,
        // but it reuses exactly the right code, so I'm going with it...
        // Note that XProc imposes the constraint that it must be possible to evaluate
        // these expressions statically.
        when (binding) {
            is InlineInstruction -> {
                val inlineReceiver = BufferingReceiver()
                val inlineStepParams = InlineStepParameters("!inline", binding.stepConfig.location,
                    emptyMap(), emptyMap(), emptyMap(), binding.valueTemplateFilter, binding.contentType, binding.encoding)
                val inlineStep = InlineStep(inlineStepParams)
                inlineStep.setup(binding.stepConfig, inlineReceiver, inlineStepParams)

                if (binding.documentProperties.canBeResolvedStatically()) {
                    inlineStep.option(Ns.documentProperties, LazyValue(binding.stepConfig, binding.documentProperties))
                } else {
                    throw XProcError.xiImpossible("Default binding can't resolve document-properties statically?").exception()
                }

                inlineStep.run()
                return inlineReceiver.outputs["result"]!!
            }
            is DocumentInstruction -> {
                val documentReceiver = BufferingReceiver()
                val documentStepParameters = DocumentStepParameters("!document", binding.stepConfig.location,
                    emptyMap(), emptyMap(), emptyMap(), binding.contentType)
                val documentStep = DocumentStep(documentStepParameters)
                documentStep.setup(binding.stepConfig, documentReceiver, documentStepParameters)

                if (binding.href.canBeResolvedStatically()) {
                    documentStep.option(Ns.href, LazyValue(binding.stepConfig, binding.href))
                } else {
                    throw XProcError.xiImpossible("Input default document href isn't static?").exception()
                }

                if (binding.documentProperties.canBeResolvedStatically()) {
                    documentStep.option(Ns.documentProperties, LazyValue(binding.stepConfig, binding.documentProperties))
                } else {
                    throw XProcError.xiImpossible("Default binding can't resolve document-properties statically?").exception()
                }

                if (binding.parameters.canBeResolvedStatically()) {
                    documentStep.option(Ns.parameters, LazyValue(binding.stepConfig, binding.parameters))
                } else {
                    throw XProcError.xiImpossible("Default binding can't resolve document-properties statically?").exception()
                }

                documentStep.run()
                return documentReceiver.outputs["result"]!!
            }
            is EmptyInstruction -> {
                return emptyList()
            }
            else -> {
                throw XProcError.xiImpossible("Unexpected default binding: ${binding}").exception()
            }
        }
    }

    override fun toString(): String {
        return "${type}/${name} (${id})"
    }
}