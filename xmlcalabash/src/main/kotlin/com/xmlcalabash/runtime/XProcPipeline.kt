package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.steps.AtomicOptionStep
import com.xmlcalabash.runtime.steps.CompoundStep
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.DefaultOutputReceiver
import com.xmlcalabash.util.SaxonValueConverter
import net.sf.saxon.s9api.QName
import java.util.*

class XProcPipeline(pipeline: CompoundStepModel) {
    val stepConfig = pipeline.stepConfig
    val inputManifold = pipeline.inputs
    val outputManifold = pipeline.outputs
    val optionManifold = pipeline.options
    val yconfig: RuntimeStepConfiguration
    val runnable: CompoundStep
    var receiver: Receiver = DefaultOutputReceiver(pipeline.stepConfig.processor)
    private val setOptions = mutableSetOf<QName>()
    private val boundInputs = mutableSetOf<String>()

    init {
        val context = RuntimeStepStaticContextImpl(pipeline.stepConfig)
        val rteContext = RuntimeExecutionContext(pipeline.stepConfig)
        val valueConverter = SaxonValueConverter(context.processor)

        rteContext.xmlCalabash.setEpisode("E-${UUID.randomUUID()}")

        yconfig = RuntimeStepConfiguration(context, rteContext, valueConverter)
        runnable = pipeline.runnable(yconfig)() as CompoundStep
        runnable.instantiate()
    }

    fun input(port: String, document: XProcDocument) {
        boundInputs.add(port)
        runnable.head.input(port, document)
    }

    fun option(name: QName, value: XProcDocument) {
        val option = optionManifold[name]
        if (option == null) {
            throw XProcError.xsNoSuchOption(name).exception()
        }
        if (name in setOptions) {
            throw XProcError.xsDuplicateOption(name).exception() // not really exactly the right error
        }
        try {
            stepConfig.checkType(name, value.value, option.asType, option.values)
        } catch (ex: Exception) {
            throw XProcError.xdBadType(value.value.toString(), option.asType.toString()).exception()
        }

        for (step in runnable.runnables.filterIsInstance<AtomicOptionStep>()) {
            if (step.externalName == name) {
                step.externalValue = value
                return
            }
        }

        throw XProcError.xiImpossible("No option step for option?").exception()
    }

    fun run() {
        val proxy = ReceiverProxy()
        for ((port, _) in outputManifold) {
            runnable.foot.receiver[port] = Pair(proxy, port)
        }

        for ((port, _) in inputManifold) {
            if (port !in boundInputs) {
                runnable.head.unboundInputs.add(port)
            }
        }

        try {
            runnable.runStep()
        } catch (e: Exception) {
            stepConfig.rteContext.xmlCalabash.discardExecutionContext()
            throw e
        }
    }

    fun reset() {
        runnable.reset()
        boundInputs.clear()
    }

    inner class ReceiverProxy(): Consumer {
        override fun input(port: String, doc: XProcDocument) {
            receiver.output(port, doc)
        }
        override fun close(port: String) {
            // nop
        }
    }
}