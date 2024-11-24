package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.io.ByteArrayOutputStream
import java.net.URI

open class CssFormatterStep(): AbstractAtomicStep() {
    companion object {
        val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
    }

    private lateinit var document: XProcDocument
    private val stylesheets = mutableListOf<XProcDocument>()
    private val extensionAttributes = mutableMapOf<QName, String>()

    override fun extensionAttributes(attributes: Map<QName, String>) {
        extensionAttributes.putAll(attributes)
    }

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            document = doc
        } else {
            stylesheets.add(doc)
        }
    }

    override fun run() {
        super.run()

        val contentType = mediaTypeBinding(Ns.contentType, MediaType.PDF)
        val parameters = qnameMapBinding(Ns.parameters)

        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        var cssManager: PagedMediaManager? = null

        val procuri = if (extensionAttributes.containsKey(NsCx.processor)) {
            logger.debug { "Searching for ${extensionAttributes[NsCx.processor]} css-formatter" }
            URI("$genericCssFormatter/${extensionAttributes[NsCx.processor]}")
        } else {
            genericCssFormatter
        }

        for (manager in managers) {
            if (manager.formatterAvailable(procuri)) {
                cssManager = manager
                break
            }
        }

        if (cssManager == null && procuri != genericCssFormatter) {
            val fallback = extensionAttributes[NsCx.fallback] ?: "true"
            if (fallback == "true" || fallback == "false") {
                if (fallback == "false") {
                    throw XProcError.xdStepFailed("Could not locate ${extensionAttributes[NsCx.processor]} css-formatter").exception()
                }
            } else {
                throw XProcError.xdBadType("cx:fallback must be 'true' or 'false'").exception()
            }

            for (manager in managers) {
                if (manager.formatterAvailable(genericCssFormatter)) {
                    cssManager = manager
                    break
                }
            }
        }

        if (cssManager == null) {
            throw XProcError.xdStepFailed("No CSS formatters available").exception()
        }

        val provider = cssManager.getCssProcessor(genericCssFormatter)

        //runtime.pipelineConfig.messageReporter.progress { "Using ${provider.name()} formatter" }

        provider.initialize(stepConfig, document.baseURI ?: stepConfig.baseUri ?: UriUtils.cwdAsUri(), parameters)
        for (stylesheet in stylesheets) {
            provider.addStylesheet(stylesheet)
        }

        val pdf = ByteArrayOutputStream()

        try {
            provider.format(document, contentType, pdf)
            receiver.output("result", XProcDocument.ofBinary(pdf.toByteArray(), stepConfig, contentType, DocumentProperties()))
        } catch (ex: Exception) {
            println(ex)
            throw ex
        }
    }

    override fun toString(): String = "p:css-formatter"
}