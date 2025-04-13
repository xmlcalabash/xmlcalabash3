package com.xmlcalabash.datamodel

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepServiceProvider
import com.xmlcalabash.spi.ConfigurerServiceProvider
import com.xmlcalabash.spi.DocumentResolverProvider
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.util.AssertionsLevel
import net.sf.saxon.s9api.QName
import java.net.URI
import java.util.*
import javax.activation.MimetypesFileTypeMap

open class CompileEnvironment(override val episode: String, override val xmlCalabash: XmlCalabash): XProcEnvironment {
    override val productName = XmlCalabashBuildConfig.PRODUCT_NAME
    override val productVersion = XmlCalabashBuildConfig.VERSION
    override val buildId = XmlCalabashBuildConfig.BUILD_ID
    override val vendor = XmlCalabashBuildConfig.VENDOR_NAME
    override val vendorUri = XmlCalabashBuildConfig.VENDOR_URI
    override val locale = Locale.getDefault().toString().replace("_", "-")
    override val version = "3.1"
    override val xpathVersion = "3.1"
    override val xmlCalabashConfig = xmlCalabash.config

    internal val _standardSteps = mutableMapOf<QName, DeclareStepInstruction>()
    override val standardSteps = _standardSteps

    internal val _defaultContentTypes = mutableMapOf<String, String>(
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
    override val contentTypes = _defaultContentTypes

    override val messagePrinter: MessagePrinter = xmlCalabash.config.messagePrinter
    override val messageReporter: MessageReporter = xmlCalabash.config.messageReporter
    override val monitors: MutableList<Monitor> = mutableListOf()
    override val documentManager: DocumentManager = DocumentManager()
    override val mimeTypes: MimetypesFileTypeMap = MimetypesFileTypeMap()
    override val errorExplanation: ErrorExplanation = DefaultErrorExplanation(messagePrinter)
    override val proxies: Map<String, String> = emptyMap()
    override val assertions: AssertionsLevel = xmlCalabash.config.assertions

    private val stepManagers = mutableListOf<AtomicStepManager>()
    private val knownAtomicSteps = mutableSetOf<QName>()

    init {
        for (provider in AtomicStepServiceProvider.providers()) {
            val manager = provider.create();
            for (stepType in manager.stepTypes()) {
                messageReporter.debug { "Added available step type '$stepType'" }
                knownAtomicSteps.add(stepType)
            }
            stepManagers.add(manager)
        }

        for ((ext, contentType) in _defaultContentTypes) {
            if (mimeTypes.getContentType("test.${ext}") == "application/octet-stream") {
                messageReporter.debug { "Assigning default content type to '.${ext}' files: ${contentType}" }
                mimeTypes.addMimeTypes("${contentType} ${ext}")
            }
        }

        for (configurer in xmlCalabash.configurers) {
            configurer.configureContentTypes(_defaultContentTypes, mimeTypes)
        }

        for ((contentType, extensions) in xmlCalabash.config.mimetypes) {
            messageReporter.debug { "Assigning content type to '${extensions}' files: ${contentType}" }
            mimeTypes.addMimeTypes("${contentType} ${extensions}")
        }

        for (provider in DocumentResolverServiceProvider.providers()) {
            val manager = provider.create();
            manager.configure(documentManager)
        }
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

    internal val nameCounts = mutableMapOf<String, Int>()
    override fun uniqueName(base: String): String {
        if (base in nameCounts) {
            val suffix = nameCounts[base]!! + 1
            nameCounts[base] = suffix
            return "${base}_${suffix}"
        }
        nameCounts[base] = 1
        return base
    }

    private val uniqueUris = mutableMapOf<String, Int>()
    override fun uniqueUri(base: String): URI {
        if (!xmlCalabashConfig.uniqueInlineUris) {
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
}