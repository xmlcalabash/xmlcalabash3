package com.xmlcalabash.runtime.steps

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.model.StepModel
import com.xmlcalabash.runtime.parameters.DocumentStepParameters
import com.xmlcalabash.runtime.parameters.InlineStepParameters
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.internal.DocumentStep
import com.xmlcalabash.steps.internal.InlineStep
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.DurationUtils
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractStep(val stepConfig: XProcStepConfiguration, step: StepModel, val type: QName = step.type, val name: String = step.name): Consumer {
    val _id: String
    init {
        if (step.instantiationCount == 1) {
            _id = step.id
        } else {
            _id = "${step.id}.${step.instantiationCount}"
        }
        step.instantiationCount++
    }
    override val id = _id

    val location = step.location
    internal val receiver = mutableMapOf<String, Pair<Consumer, String>>()
    val inputCount = mutableMapOf<String, Int>()
    val outputCount = mutableMapOf<String, Int>()
    val staticOptions = step.staticOptions.toMutableMap()
    val verbosity = stepConfig.saxonConfig.xmlCalabash.xmlCalabashConfig.verbosity

    var aborted = false
    abstract val stepTimeout: Duration
    abstract val params: RuntimeStepParameters
    abstract val readyToRun: Boolean
    abstract fun output(port: String, document: XProcDocument)
    abstract fun instantiate()
    abstract fun prepare()
    abstract fun run()

    open fun abort() {
        aborted = true
    }

    internal fun checkInputPort(port: String, doc: XProcDocument, flange: RuntimePort?): XProcError? {
        if (flange == null) {
            stepConfig.warn { "Unexpected input port: ${port} on ${this}" }
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
            stepConfig.warn { "Unexpected input port: ${port} on ${this}" }
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
            stepConfig.warn { "Unexpected output port: ${port} on ${this}" }
            return
        }

        val count = (outputCount[port] ?: 0) + 1
        if (!flange.sequence && count != 1) {
            if (type == NsCx.select) {
                // This is really an error on the input we're feeding into
                throw stepConfig.exception(XProcError.xdInputSequenceForbidden(port))
            }
            throw stepConfig.exception(XProcError.xdOutputSequenceForbidden(port))
        }

        if (flange.contentTypes.isNotEmpty()) {
            val mtype = doc.contentType ?: MediaType.ANY
            val match = mtype.matchingMediaType(flange.contentTypes)
            if (match == null || !match.inclusive) {
                throw stepConfig.exception(XProcError.xdBadOutputContentType(port, mtype.toString()))
            }
        }

        outputCount[port] = count
        //println("output ${port}: on ${this}")
    }

    open fun runStep() {
        logger.debug { "Running ${this}" }

        try {
            prepare()

            for (monitor in stepConfig.environment.monitors) {
                monitor.startStep(this)
            }

            if (stepTimeout.toMillis() > 0) {
                val service = Executors.newSingleThreadExecutor()
                val future = service.submit(RunTask())
                try {
                    future.get(stepTimeout.toMillis(), TimeUnit.MILLISECONDS)
                } catch (ex: TimeoutException) {
                    future.cancel(true)
                    service.shutdownNow()
                    throw stepConfig.exception(XProcError.xdStepTimeout(DurationUtils.prettyPrint(stepTimeout)))
                }
                service.shutdownNow()
            } else {
                run()
            }

            for (monitor in stepConfig.environment.monitors) {
                monitor.endStep(this)
            }
        } catch (ex: Exception) {
            for (monitor in stepConfig.environment.monitors) {
                monitor.abortStep(this, ex)
            }

            when (ex) {
                is XProcException -> {
                    ex.error.at(type, name).at(location)
                    throw ex
                }
                else -> {
                    val msg = ex.message ?: ""
                    if (msg.contains("cannot be cast")) {
                        throw stepConfig.exception(XProcError.xdBadType(msg).at(type,name))
                    }
                    throw stepConfig.exception(XProcError.xdStepFailed(msg).at(type, name))
                }
            }
        }
    }

    open fun reset() {
        aborted = false
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
                val source = RuntimePort("source", false, true, true, emptyList())
                val result = RuntimePort("result", false, true, false, emptyList())
                val inlineStepParams = InlineStepParameters("!inline", binding.stepConfig.location,
                    mapOf("source" to source), mapOf("result" to result), emptyMap(), binding.valueTemplateFilter, binding.contentType, binding.encoding)
                val inlineStep = InlineStep(inlineStepParams)
                inlineStep.setup(binding.stepConfig, inlineReceiver, inlineStepParams)

                if (binding.documentProperties.canBeResolvedStatically()) {
                    inlineStep.option(Ns.documentProperties, LazyValue(binding.stepConfig, binding.documentProperties, stepConfig))
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Default binding can't resolve document-properties statically?"))
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
                    documentStep.option(Ns.href, LazyValue(binding.stepConfig, binding.href, stepConfig))
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Input default document href isn't static?"))
                }

                if (binding.documentProperties.canBeResolvedStatically()) {
                    documentStep.option(Ns.documentProperties, LazyValue(binding.stepConfig, binding.documentProperties, stepConfig))
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Default binding can't resolve document-properties statically?"))
                }

                if (binding.parameters.canBeResolvedStatically()) {
                    documentStep.option(Ns.parameters, LazyValue(binding.stepConfig, binding.parameters, stepConfig))
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Default binding can't resolve document-properties statically?"))
                }

                documentStep.run()
                return documentReceiver.outputs["result"]!!
            }
            is EmptyInstruction -> {
                return emptyList()
            }
            else -> {
                throw stepConfig.exception(XProcError.xiImpossible("Unexpected default binding: ${binding}"))
            }
        }
    }

    override fun toString(): String {
        return "${type}/${name} (${id})"
    }

    inner class RunTask: Callable<Unit> {
        override fun call() {
            run()
        }
    }
}