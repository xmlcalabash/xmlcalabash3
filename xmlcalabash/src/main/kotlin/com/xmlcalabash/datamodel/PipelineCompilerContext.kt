package com.xmlcalabash.datamodel

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.config.CommonEnvironment
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.debugger.CliDebugger
import com.xmlcalabash.debugger.Debugger
import com.xmlcalabash.debugger.NopDebugger
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.Monitor
import com.xmlcalabash.runtime.XProcExecutionContext
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.tracing.NopTraceListener
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.MessageReporter
import com.xmlcalabash.util.SchematronMonitor
import java.net.URI
import java.util.*
import javax.activation.MimetypesFileTypeMap

class PipelineCompilerContext(override val xmlCalabash: XmlCalabash): PipelineEnvironment {
    companion object {
        private var _id = 0L
    }

    override val commonEnvironment: CommonEnvironment
        get() = xmlCalabash.commonEnvironment

    override val standardSteps = commonEnvironment.standardSteps
    override val episode: String = ""
    override val locale = Locale.getDefault().toString().replace("_", "-")
    override val productName = XmlCalabashBuildConfig.PRODUCT_NAME
    override val productVersion = XmlCalabashBuildConfig.VERSION
    override val gitHash = XmlCalabashBuildConfig.BUILD_HASH
    override val vendor = XmlCalabashBuildConfig.VENDOR_NAME
    override val vendorUri = XmlCalabashBuildConfig.VENDOR_URI
    override val version = "3.0"
    override val xpathVersion = "3.1"
    override var uniqueInlineUris = true

    private var _traceListener = NopTraceListener()
    private var _documentManager: DocumentManager = commonEnvironment.documentManager
    private var _mimeTypes = commonEnvironment.mimeTypes
    private var _errorExplanation: ErrorExplanation = commonEnvironment.errorExplanation
    private var _messageReporter: MessageReporter = commonEnvironment.messageReporter

    private var _proxies = mutableMapOf<String, String>()
    init {
        _proxies.putAll(commonEnvironment.proxies)
    }

    override val nextId: String
        get() {
            synchronized(Companion) {
                return "IC${++_id}"
            }
        }

    // In this object, we return unique step names
    private val nameCounts = mutableMapOf<String, Int>()
    override fun uniqueName(base: String): String {
        if (base in nameCounts) {
            val suffix = nameCounts[base]!! + 1
            nameCounts[base] = suffix
            return "${base}_${suffix}"
        }
        nameCounts[base] = 1
        return base
    }

    override val monitors: List<Monitor> = emptyList()
    override val debugger: Debugger = NopDebugger() // The debugger is never used at compile time...
    override val traceListener: TraceListener
        get() = _traceListener
    override val documentManager: DocumentManager
        get() = _documentManager
    override val mimeTypes: MimetypesFileTypeMap
        get() = _mimeTypes
    override val errorExplanation: ErrorExplanation
        get() = _errorExplanation
    override val messageReporter: MessageReporter
        get() = _messageReporter
    override val proxies: Map<String, String>
        get() = _proxies

    override fun uniqueUri(base: String): URI {
        return commonEnvironment.uniqueUri(base)
    }

    override fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext {
        return commonEnvironment.newExecutionContext(stepConfig)
    }

    override fun getExecutionContext(): XProcExecutionContext {
        return commonEnvironment.getExecutionContext()
    }

    override fun setExecutionContext(dynamicContext: XProcExecutionContext) {
        return commonEnvironment.setExecutionContext(dynamicContext)
    }

    override fun releaseExecutionContext() {
        return commonEnvironment.releaseExecutionContext()
    }

    override fun addProperties(doc: XProcDocument?) {
        return commonEnvironment.addProperties(doc)
    }

    override fun removeProperties(doc: XProcDocument?) {
        return commonEnvironment.removeProperties(doc)
    }
}