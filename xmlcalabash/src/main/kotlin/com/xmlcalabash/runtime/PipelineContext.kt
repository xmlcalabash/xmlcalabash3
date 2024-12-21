package com.xmlcalabash.runtime

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.PipelineCompilerContext
import com.xmlcalabash.datamodel.PipelineEnvironment
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.tracing.DetailTraceListener
import com.xmlcalabash.tracing.NopTraceListener
import com.xmlcalabash.tracing.StandardTraceListener
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.MessageReporter
import net.sf.saxon.s9api.QName
import java.net.URI
import java.util.*
import javax.activation.MimetypesFileTypeMap

class PipelineContext(compilerContext: PipelineCompilerContext): PipelineEnvironment {
    companion object {
        internal var _id = 0L
    }

    override val commonEnvironment = compilerContext.commonEnvironment
    override val standardSteps = compilerContext.standardSteps
    override val xmlCalabash: XmlCalabash = compilerContext.xmlCalabash
    override val episode: String = "E-${UUID.randomUUID()}"
    override val locale = Locale.getDefault().toString().replace("_", "-")
    override val productName = XmlCalabashBuildConfig.PRODUCT_NAME
    override val productVersion = XmlCalabashBuildConfig.VERSION
    override val gitHash = XmlCalabashBuildConfig.BUILD_HASH
    override val vendor = XmlCalabashBuildConfig.VENDOR_NAME
    override val vendorUri = XmlCalabashBuildConfig.VENDOR_URI
    override val version = "3.0"
    override val xpathVersion = "3.1"
    override var uniqueInlineUris = compilerContext.uniqueInlineUris

    private var _traceListener = if (xmlCalabash.xmlCalabashConfig.trace != null
        || xmlCalabash.xmlCalabashConfig.traceDocuments != null) {
        if (xmlCalabash.xmlCalabashConfig.traceDocuments != null) {
            DetailTraceListener()
        } else {
            StandardTraceListener()
        }
    } else {
        NopTraceListener()
    }
    private var _documentManager = compilerContext.documentManager
    private var _mimeTypes = compilerContext.mimeTypes
    private var _errorExplanation = compilerContext.errorExplanation
    private var _messageReporter = compilerContext.messageReporter
    private var _proxies = mutableMapOf<String, String>()

    init {
        _proxies.putAll(compilerContext.proxies)
    }

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

    override val nextId: String
        get() {
            synchronized(Companion) {
                return "IR${++_id}"
            }
        }

    override fun uniqueUri(base: String): URI {
        return commonEnvironment.uniqueUri(base)
    }

    fun stepProvider(params: StepParameters): () -> XProcStep {
        return commonEnvironment.stepProvider(params)
    }

    fun atomicStepAvailable(type: QName): Boolean {
        return commonEnvironment.atomicStepAvailable(type)
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