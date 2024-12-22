package com.xmlcalabash.runtime

import com.xmlcalabash.debugger.CliDebugger
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.steps.AtomicOptionStep
import com.xmlcalabash.runtime.steps.CompoundStep
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.DefaultOutputReceiver
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import org.apache.logging.log4j.kotlin.logger
import java.io.FileOutputStream

class XProcPipeline internal constructor(private val runtime: XProcRuntime, pipeline: CompoundStepModel, val config: XProcStepConfiguration) {
    val inputManifold = pipeline.inputs
    val outputManifold = pipeline.outputs
    val optionManifold = pipeline.options
    val runnable: CompoundStep
    var receiver: Receiver = DefaultOutputReceiver(pipeline.stepConfig)
    private val setOptions = mutableSetOf<QName>()
    private val boundInputs = mutableSetOf<String>()

    init {
        runnable = pipeline.runnable(config)() as CompoundStep
        runnable.instantiate()

        if (pipeline.stepConfig.environment.xmlCalabash.xmlCalabashConfig.debugger) {
            if (config.environment is PipelineContext) {
                val debugger = CliDebugger(runtime)
                (config.environment as PipelineContext)._debugger = debugger
            } else {
                logger.debug { "Cannot instantiate debugger on ${config.environment}" }
            }
        }
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
            config.checkType(name, value.value, option.asType, config.inscopeNamespaces, option.values)
        } catch (ex: Exception) {
            throw XProcError.xdBadType(value.value.toString(), option.asType.toString()).exception()
        }

        for (step in runnable.runnables.filterIsInstance<AtomicOptionStep>()) {
            if (step.externalName == name) {
                step.externalValue = value
                return
            }
        }

        // Static options won't have runnables, so they're okay.
        if (!option.static) {
            throw XProcError.xiImpossible("No option step for option?").exception()
        }
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
            config.xmlCalabash.discardExecutionContext()
            throw e
        }

        val trace = config.environment.xmlCalabash.xmlCalabashConfig.trace
        if (trace != null) {
            val props = DocumentProperties()
            val serial = config.asXdmMap(mapOf(
                Ns.method to XdmAtomicValue("xml"),
                Ns.omitXmlDeclaration to XdmAtomicValue(true),
                Ns.indent to XdmAtomicValue(true)
            ))
            props.set(Ns.serialization, serial)
            val doc = XProcDocument.ofXml(config.environment.traceListener.summary(config), config, props)
            val serializer = XProcSerializer(config)
            val fileOutputStream = FileOutputStream(trace)
            serializer.write(doc, fileOutputStream)
            fileOutputStream.close()
        }
    }

    fun reset() {
        runnable.reset()
        boundInputs.clear()
    }

    inner class ReceiverProxy(): Consumer {
        override val id = "PIPELINE"

        override fun input(port: String, doc: XProcDocument) {
            receiver.output(port, doc)
        }

        override fun close(port: String) {
            // nop
        }
    }
}