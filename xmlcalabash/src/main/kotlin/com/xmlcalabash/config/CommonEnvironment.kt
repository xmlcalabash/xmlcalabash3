package com.xmlcalabash.config

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.XProcExecutionContext
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepServiceProvider
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.util.DefaultMessageReporter
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.util.DefaultMessagePrinter
import com.xmlcalabash.util.InternalDocumentResolver
import net.sf.saxon.lib.Initializer
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.net.URI
import javax.activation.MimetypesFileTypeMap

class CommonEnvironment(private val xmlCalabash: XmlCalabash) {
    companion object {
        // N.B. This is used in reverse in MediaType.extension()
        private val standardDefaultContentTypes = mapOf(
            "7z" to "application/x-7z-compressed",
            "a" to "application/x-archive",
            "arj" to "application/x-arj",
            "bmp" to "image/bmp",
            "bz2" to "application/bzip2",
            "cpio" to "application/x-cpio",
            "css" to "text/plain",
            "csv" to "text/csv",
            "dtd" to "application/xml-dtd",
            "eps" to "image/eps",
            "fo" to "application/xml",
            "gif" to "image/gif",
            "gz" to "application/gzip",
            "gzip" to "application/gzip",
            "jar" to "application/java-archive",
            "jpeg" to "image/jpeg",
            "jpg" to "image/jpeg",
            "json" to "application/json",
            "lzma" to "application/lzma",
            "nvdl" to "application/nvdl+xml",
            "pdf" to "application/pdf",
            "rnc" to "application/relax-ng-compact-syntax",
            "rng" to "application/relax-ng+xml",
            "sch" to "application/schematron+xml",
            "svg" to "image/svg+xml",
            "tar" to "application/x-tar",
            "text" to "text/plain",
            "toml" to "application/toml",
            "txt" to "text/plain",
            "xml" to "application/xml",
            "xpl" to "application/xproc+xml",
            "xq" to "application/xquery",
            "xqy" to "application/xquery",
            "xsd" to "application/xsd+xml",
            "xsl" to "application/xslt+xml",
            "xslt" to "application/xslt+xml",
            "xz" to "application/xz",
            "yml" to "application/x-yaml",
            "yaml" to "application/x-yaml",
            "zip" to "application/zip")
    }

    private var _documentManager: DocumentManager = DocumentManager()
    private var _messagePrinter: MessagePrinter = xmlCalabash.xmlCalabashConfig.messagePrinter
    private var _errorExplanation: ErrorExplanation = DefaultErrorExplanation(_messagePrinter)
    private var _messageReporter: (() -> MessageReporter)? = null
    private var _proxies = mutableMapOf<String, String>()
    private val _additionalInitializers = mutableListOf<Initializer>()
    internal val _standardSteps = mutableMapOf<QName, DeclareStepInstruction>()
    private val _defaultContentTypes = mutableMapOf<String, String>()

    private val uniqueUris = mutableMapOf<String, Int>()
    private val stepManagers = mutableListOf<AtomicStepManager>()
    private val knownAtomicSteps = mutableSetOf<QName>()

    val mimeTypes = MimetypesFileTypeMap()

    val defaultContentTypes: Map<String, String> = _defaultContentTypes

    init {
        errorExplanation.setEnvironment(this)

        if (xmlCalabash.xmlCalabashConfig.pipe) {
            xmlCalabash.xmlCalabashConfig.messagePrinter.setPrintStream(System.err)
        }

        for ((contentType, extensions) in xmlCalabash.xmlCalabashConfig.mimetypes) {
            mimeTypes.addMimeTypes("${contentType} ${extensions}")
        }

        _defaultContentTypes.putAll(standardDefaultContentTypes)
        for ((ext, contentType) in defaultContentTypes) {
            if (mimeTypes.getContentType("test.${ext}") == "application/octet-stream") {
                logger.trace { "Assigning default content type to '.${ext}' files: ${contentType}" }
                mimeTypes.addMimeTypes("${contentType} ${ext}")
            }
        }

        for (provider in AtomicStepServiceProvider.providers()) {
            val manager = provider.create();
            for (stepType in manager.stepTypes()) {
                logger.trace { "Added available step type '$stepType'" }
                knownAtomicSteps.add(stepType)
            }
            stepManagers.add(manager)
        }

        val internalManager = InternalDocumentResolver()
        for (uri in internalManager.resolvableUris()) {
            logger.trace { "Added resolvable URI: ${uri}" }
        }
        internalManager.configure(_documentManager)

        for (provider in DocumentResolverServiceProvider.providers()) {
            val manager = provider.create();
            for (uri in manager.resolvableUris()) {
                logger.trace { "Added resolvable URI: ${uri}" }
            }
            manager.configure(_documentManager)
        }
    }

    val config = xmlCalabash.xmlCalabashConfig
    var eagerEvaluation = false
    var documentManager: DocumentManager
        get() = _documentManager
        set(value) {
            _documentManager = value
            for (provider in DocumentResolverServiceProvider.providers()) {
                provider.create().configure(_documentManager)
            }
        }
    var errorExplanation: ErrorExplanation
        get() = _errorExplanation
        set(value) {
            _errorExplanation = value
        }
    val messagePrinter = _messagePrinter
    var messageReporter: () -> MessageReporter
        get() {
            if (_messageReporter == null) {
                _messageReporter = { DefaultMessageReporter(xmlCalabash.xmlCalabashConfig.messagePrinter) }
            }
            return _messageReporter!!
        }
        set(value) {
            _messageReporter = value
        }
    val proxies: Map<String, String>
        get() = _proxies
    val standardSteps: Map<QName, DeclareStepInstruction>
        get() = _standardSteps

    val additionalInitializers: List<Initializer>
        get() = _additionalInitializers

    fun configure(configurer: Configurer) {
        configurer.configureContentTypes(_defaultContentTypes, mimeTypes)
    }

    fun addInitializer(initializer: Initializer) {
        _additionalInitializers.add(initializer)
    }

    fun uniqueUri(base: String): URI {
        if (!xmlCalabash.xmlCalabashConfig.uniqueInlineUris) {
            return URI(base)
        }
        synchronized(uniqueUris) {
            val count = uniqueUris[base] ?: 0
            uniqueUris[base] = count + 1
            if (count == 0 && base != "") {
                return URI(base)
            }
            return URI("${base}?uniqueid=${count}")
        }
    }

    fun stepProvider(params: StepParameters): () -> XProcStep {
        for (manager in stepManagers) {
            if (manager.stepAvailable(params.stepType)) {
                return manager.createStep(params)
            }
        }
        throw XProcError.xsMissingStepDeclaration(params.stepType).exception()
    }

    fun atomicStepAvailable(type: QName): Boolean {
        return knownAtomicSteps.contains(type)
    }

    internal fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext {
        return xmlCalabash.newExecutionContext(stepConfig)
    }

    internal fun getExecutionContext(): XProcExecutionContext {
        return xmlCalabash.getExecutionContext()
    }

    internal fun setExecutionContext(dynamicContext: XProcExecutionContext) {
        return xmlCalabash.setExecutionContext(dynamicContext)
    }

    internal fun releaseExecutionContext() {
        return xmlCalabash.releaseExecutionContext()
    }

    internal fun discardExecutionContext() {
        return xmlCalabash.discardExecutionContext()
    }

    internal fun addProperties(doc: XProcDocument?) {
        return xmlCalabash.addProperties(doc)
    }

    internal fun removeProperties(doc: XProcDocument?) {
        return xmlCalabash.removeProperties(doc)
    }
}