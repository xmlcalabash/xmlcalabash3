package com.xmlcalabash.runtime

import com.xmlcalabash.debugger.CliDebugger
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.steps.AtomicOptionStep
import com.xmlcalabash.runtime.steps.CompoundStep
import com.xmlcalabash.tracing.DetailTraceListener
import com.xmlcalabash.tracing.StandardTraceListener
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.DefaultOutputReceiver
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.AssertionsMonitor
import com.xmlcalabash.visualizers.Silent
import net.sf.saxon.s9api.QName
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths

class XProcPipeline internal constructor(runtime: XProcRuntime, pipeline: CompoundStepModel, val config: XProcStepConfiguration) {
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
        val monitors = (config.environment as PipelineContext).monitors

        if (xconfig.debugger) {
            if (config.environment is PipelineContext) {
                val debugger = CliDebugger(runtime)
                monitors.add(debugger)
            } else {
                pipeline.stepConfig.warn { "Cannot provide debugger on ${config.environment}" }
            }
        }

        if (config.environment.assertions != AssertionsLevel.IGNORE) {
            if (config.environment is PipelineContext) {
                monitors.add(AssertionsMonitor())
            } else {
                pipeline.stepConfig.warn { "Cannot provide Schematron monitor on ${config.environment}" }
            }
        }

        if (xconfig.trace != null || xconfig.traceDocuments != null) {
            if (config.environment is PipelineContext) {
                traceListener = if (xconfig.traceDocuments != null) {
                    DetailTraceListener(xconfig.traceDocuments!!.toPath())
                } else {
                    StandardTraceListener()
                }
                monitors.add(traceListener!!)
            } else {
                pipeline.stepConfig.warn { "Cannot provide tracing on ${config.environment}" }
            }
        }

        if (xconfig.visualizer !is Silent) {
            monitors.add(xconfig.visualizer)
        }
    }

    fun input(port: String, document: XProcDocument) {
        if ((!runnable.params.inputs.containsKey(port))) {
            throw XProcError.xsNoSuchPort(port).exception()
        }

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
        } catch (_: Exception) {
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
        for ((port, manifold) in outputManifold) {
            runnable.foot.receiver[port] = Pair(proxy, port)
            proxy.serialization(port, manifold.serialization)
        }

        for ((port, _) in inputManifold) {
            if (port !in boundInputs) {
                runnable.head.unboundInputs.add(port)
            }
        }

        val executionContext = config.xmlCalabash.preserveExecutionContext()
        try {
            runnable.runStep()
            config.xmlCalabash.restoreExecutionContext(executionContext)
        } catch (e: Exception) {
            config.xmlCalabash.restoreExecutionContext(executionContext)
            throw e
        }

        val trace = config.environment.xmlCalabash.xmlCalabashConfig.trace
        if (trace != null && traceListener != null) {
            trace.parentFile.mkdirs()
            val doc = XProcDocument.ofXml(traceListener!!.summary(config), config)
            val fos = FileOutputStream(trace)
            val writer = DocumentWriter(doc, fos)
            writer[Ns.method] = "xml"
            writer[Ns.omitXmlDeclaration] = true
            writer[Ns.indent] = true
            writer.write()
            fos.close()
        }
    }

    fun reset() {
        runnable.reset()
        boundInputs.clear()
    }
}