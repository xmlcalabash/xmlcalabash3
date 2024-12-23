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
import com.xmlcalabash.tracing.DetailTraceListener
import com.xmlcalabash.tracing.StandardTraceListener
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.DefaultOutputReceiver
import com.xmlcalabash.util.SchematronAssertions
import com.xmlcalabash.util.SchematronMonitor
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
    private var traceListener: TraceListener? = null

    init {
        runnable = pipeline.runnable(config)() as CompoundStep
        runnable.instantiate()

        val xconfig = pipeline.stepConfig.xmlCalabash.xmlCalabashConfig

        if (xconfig.debugger) {
            if (config.environment is PipelineContext) {
                val debugger = CliDebugger(runtime)
                (config.environment as PipelineContext).monitors.add(debugger)
            } else {
                logger.debug { "Cannot provide debugger on ${config.environment}" }
            }
        }

        if (xconfig.assertions != SchematronAssertions.IGNORE) {
            if (config.environment is PipelineContext) {
                (config.environment as PipelineContext).monitors.add(SchematronMonitor())
            } else {
                logger.debug { "Cannot provide Schematron monitor on ${config.environment}" }
            }
        }

        if (xconfig.trace != null || xconfig.traceDocuments != null) {
            if (config.environment is PipelineContext) {
                traceListener = if (xconfig.traceDocuments != null) {
                    DetailTraceListener()
                } else {
                    StandardTraceListener()
                }
                (config.environment as PipelineContext).monitors.add(traceListener!!)
            } else {
                logger.debug { "Cannot provide tracing on ${config.environment}" }
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
        val proxy = PipelineReceiverProxy(receiver)
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

            if (traceListener != null) {
                val doc = XProcDocument.ofXml(traceListener!!.summary(config), config, props)
                val serializer = XProcSerializer(config)
                val fileOutputStream = FileOutputStream(trace)
                serializer.write(doc, fileOutputStream)
                fileOutputStream.close()
            }
        }
    }

    fun reset() {
        runnable.reset()
        boundInputs.clear()
    }
}