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

open class XslFormatterStep(): AbstractAtomicStep() {
    companion object {
        val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
    }

    private lateinit var document: XProcDocument
    private val extensionAttributes = mutableMapOf<QName, String>()

    override fun extensionAttributes(attributes: Map<QName, String>) {
        extensionAttributes.putAll(attributes)
    }

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val contentType = mediaTypeBinding(Ns.contentType, MediaType.PDF)
        val parameters = qnameMapBinding(Ns.parameters)

        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }

        var xslManager: PagedMediaManager? = null

        val procuri = if (extensionAttributes.containsKey(NsCx.processor)) {
            logger.debug { "Searching for ${extensionAttributes[NsCx.processor]} xsl-formatter" }
            URI("${genericXslFormatter}/${extensionAttributes[NsCx.processor]}")
        } else {
            genericXslFormatter
        }

        for (manager in managers) {
            if (manager.formatterAvailable(procuri)) {
                xslManager = manager
                break
            }
        }

        if (xslManager == null && procuri != genericXslFormatter) {
            val fallback = extensionAttributes[NsCx.fallback] ?: "true"
            if (fallback == "true" || fallback == "false") {
                if (fallback == "false") {
                    throw XProcError.xdStepFailed("Could not locate ${extensionAttributes[NsCx.processor]} xsl-formatter").exception()
                }
            } else {
                throw XProcError.xdBadType("cx:fallback must be 'true' or 'false'").exception()
            }

            for (manager in managers) {
                if (manager.formatterAvailable(genericXslFormatter)) {
                    xslManager = manager
                    break
                }
            }
        }

        if (xslManager == null) {
            throw XProcError.xdStepFailed("No XSL formatters available").exception()
        }

        val provider = xslManager.getFoProcessor(genericXslFormatter)

        // FIXME:
        //runtime.pipelineConfig.messageReporter.progress { "Using ${provider.name()} formatter" }

        provider.initialize(stepConfig, document.baseURI ?: stepConfig.baseUri ?: UriUtils.cwdAsUri(), parameters)

        val pdf = ByteArrayOutputStream()
        provider.format(document, contentType, pdf)

        receiver.output("result", XProcDocument.ofBinary(pdf.toByteArray(), stepConfig, contentType, DocumentProperties()))
    }

    override fun toString(): String = "p:xsl-formatter"
}