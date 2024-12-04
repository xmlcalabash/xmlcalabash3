package com.xmlcalabash.runtime

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.ExecutionContext
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepServiceProvider
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.util.DefaultMessageReporter
import com.xmlcalabash.util.MessageReporter
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.net.URI
import java.util.*
import javax.activation.MimetypesFileTypeMap

class RuntimeExecutionContext(val xmlCalabash: XmlCalabash): ExecutionContext {
    constructor(stepConfig: StepConfiguration): this(stepConfig.saxonConfig.xmlCalabash)

    companion object {
        val defaultContentTypes = mapOf(
            "bz2" to "application/bzip2",
            "css" to "text/plain",
            "fo" to "application/xml",
            "gz" to "application/gzip",
            "gzip" to "application/gzip",
            "json" to "application/json",
            "lzma" to "application/lzma",
            "nvdl" to "application/nvdl+xml",
            "rnc" to "application/relax-ng-compact-syntax",
            "rng" to "application/relax-ng+xml",
            "sch" to "application/schematron+xml",
            "text" to "text/plain",
            "txt" to "text/plain",
            "xml" to "application/xml",
            "xpl" to "application/xml",
            "xq" to "application/xquery",
            "xqy" to "application/xquery",
            "xsd" to "application/xsd+xml",
            "xsl" to "application/xslt+xml",
            "xslt" to "application/xslt+xml",
            "xz" to "application/xz",
            "zip" to "application/zip")
    }

    override val locale = Locale.getDefault().toString().replace("_", "-")
    override val productName = XmlCalabashBuildConfig.PRODUCT_NAME
    override val productVersion = XmlCalabashBuildConfig.VERSION
    override val vendor = XmlCalabashBuildConfig.VENDOR_NAME
    override val vendorUri = XmlCalabashBuildConfig.VENDOR_URI
    override val version = "3.0"
    override val xpathVersion = "3.1"

    // N.B. This relies on proper initialization of the thread-local episode on XmlCalabash
    override val episode: String
        get() = xmlCalabash.getEpisode()

    private var _documentManager: DocumentManager = DocumentManager()
    private var _mimeTypes = MimetypesFileTypeMap()
    private var _errorExplanation: ErrorExplanation = DefaultErrorExplanation()
    private var _messageReporter: MessageReporter = DefaultMessageReporter(Verbosity.NORMAL)
    private var _proxies = mutableMapOf<String, String>()

    private val uniqueUris = mutableMapOf<String, Int>()
    private val stepManagers = mutableListOf<AtomicStepManager>()
    private val knownAtomicSteps = mutableSetOf<QName>()

    init {
        for ((contentType, extensions) in xmlCalabash.xmlCalabashConfig.mimetypes) {
            _mimeTypes.addMimeTypes("${contentType} ${extensions}")
        }

        for ((ext, contentType) in defaultContentTypes) {
            if (mimeTypes.getContentType("test.${ext}") == "application/octet-stream") {
                logger.debug { "Assigning default content type to '.${ext}' files: ${contentType}" }
                mimeTypes.addMimeTypes("${contentType} ${ext}")
            }
        }

        for (provider in AtomicStepServiceProvider.providers()) {
            val manager = provider.create();
            knownAtomicSteps.addAll(manager.stepTypes())
            stepManagers.add(manager)
        }

        for (provider in DocumentResolverServiceProvider.providers()) {
            provider.create().configure(documentManager)
        }
    }

    override var documentManager: DocumentManager
        get() = _documentManager
        set(value) {
            _documentManager = value
        }

    override var mimeTypes: MimetypesFileTypeMap
        get() = _mimeTypes
        set(value) {
            _mimeTypes = value
        }

    override var errorExplanation: ErrorExplanation
        get() = _errorExplanation
        set(value) {
            _errorExplanation = value
        }

    override var messageReporter: MessageReporter
        get() = _messageReporter
        set(value) {
            _messageReporter = value
        }

    override var proxies: Map<String, String>
        get() = _proxies
        set(value) {
            _proxies.clear()
            _proxies.putAll(value)
        }

    override fun stepProvider(params: StepParameters): () -> XProcStep {
        for (manager in stepManagers) {
            if (manager.stepAvailable(params.stepType)) {
                return manager.createStep(params)
            }
        }
        throw XProcError.xsMissingStepDeclaration(params.stepType).exception()
    }

    override fun atomicStepAvailable(type: QName): Boolean {
        return knownAtomicSteps.contains(type)
    }

    override fun uniqueUri(base: String): URI {
        if (!xmlCalabash.xmlCalabashConfig.uniqueInlineUris) {
            return URI(base)
        }
        synchronized(StepConfiguration) {
            val count = uniqueUris[base] ?: 0
            uniqueUris[base] = count + 1
            if (count == 0 && base != "") {
                return URI(base)
            }
            return URI("${base}?uniqueid=${count}")
        }
    }

    // ============================================================

    override fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext {
        return xmlCalabash.newExecutionContext(stepConfig)
    }

    override fun getExecutionContext(): XProcExecutionContext {
        return xmlCalabash.getExecutionContext()
    }

    override fun setExecutionContext(dynamicContext: XProcExecutionContext) {
        xmlCalabash.setExecutionContext(dynamicContext)
    }

    override fun releaseExecutionContext() {
        xmlCalabash.releaseExecutionContext()
    }

    override fun addProperties(doc: XProcDocument?) {
        xmlCalabash.addProperties(doc)
    }

    override fun removeProperties(doc: XProcDocument?) {
        xmlCalabash.removeProperties(doc)
    }
}