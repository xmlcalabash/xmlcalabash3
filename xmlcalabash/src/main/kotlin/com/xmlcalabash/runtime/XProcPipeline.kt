package com.xmlcalabash.runtime

import com.xmlcalabash.debugger.CliDebugger
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.TypeUtils
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
import com.xmlcalabash.visualizers.Detail
import com.xmlcalabash.visualizers.Plain
import com.xmlcalabash.visualizers.Silent
import net.sf.saxon.s9api.QName
import java.io.FileOutputStream

class XProcPipeline internal constructor(runtime: XProcRuntime, pipeline: CompoundStepModel, val config: XProcStepConfiguration) {
    val inputManifold = pipeline.inputs
    val outputManifold = pipeline.outputs
    val optionManifold = pipeline.options
    val runnable: CompoundStep
    var receiver: Receiver = DefaultOutputReceiver(pipeline.stepConfig, outputManifold)
    private val setOptions = mutableSetOf<QName>()
    private val boundInputs = mutableSetOf<String>()
    private var traceListener: TraceListener? = null

    init {
        runnable = pipeline.runnable(config)() as CompoundStep
        runnable.instantiate()

        val monitors = config.environment.monitors

        if (config.xmlCalabashConfig.debugger) {
            val debugger = CliDebugger(runtime)
            monitors.add(debugger)
        }

        if (config.environment.assertions != AssertionsLevel.IGNORE) {
            monitors.add(AssertionsMonitor())
        }

        if (config.xmlCalabashConfig.trace != null || config.xmlCalabashConfig.traceDocuments != null) {
            traceListener = if (config.xmlCalabashConfig.traceDocuments != null) {
                DetailTraceListener(config.environment, config.xmlCalabashConfig.traceDocuments!!.toPath())
            } else {
                StandardTraceListener()
            }
            monitors.add(traceListener!!)
        }

        when (config.xmlCalabashConfig.visualizer) {
            "detail" -> monitors.add(Detail(config.xmlCalabashConfig.messagePrinter, config.xmlCalabashConfig.visualizerProperties))
            "plain" -> monitors.add(Plain(config.xmlCalabashConfig.messagePrinter, config.xmlCalabashConfig.visualizerProperties))
            "silent" -> monitors.add(Silent(emptyMap()))
            else -> Unit
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
        setOptions.add(name)

        try {
            config.typeUtils.checkType(name, value.value, option.asType, config.inscopeNamespaces, option.values)
        } catch (_: Exception) {
            throw XProcError.xdBadType(value.value.toString(), TypeUtils.sequenceTypeToString(option.asType)).exception()
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

        for ((port, manifold) in inputManifold) {
            if (port !in boundInputs) {
                if (!manifold.sequence && manifold.defaultBindings.isEmpty()) {
                    throw XProcError.xdInputRequiredOnPort(port).exception()
                }
                runnable.head.unboundInputs.add(port)
            }
        }

        for ((name, manifold) in optionManifold) {
            if (name !in setOptions && manifold.required) {
                throw XProcError.xsMissingRequiredOption(name).exception()
            }
        }

        val executionContext = config.saxonConfig.preserveExecutionContext()
        try {
            runnable.runStep()
            config.saxonConfig.restoreExecutionContext(executionContext)
        } catch (e: Exception) {
            config.saxonConfig.restoreExecutionContext(executionContext)
            throw e
        }

        val trace = config.xmlCalabashConfig.trace
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