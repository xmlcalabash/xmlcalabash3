package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.spi.StandardPagedMediaProvider
import net.sf.saxon.s9api.QName
import java.io.ByteArrayOutputStream
import java.net.URI

open class XslFormatterStep(): AbstractAtomicStep() {
    companion object {
        val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
    }

    private val extensionAttributes = mutableMapOf<QName, String>()

    override fun extensionAttributes(attributes: Map<QName, String>) {
        extensionAttributes.putAll(attributes)
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val contentType = mediaTypeBinding(Ns.contentType, MediaType.PDF)
        val parameters = qnameMapBinding(Ns.parameters)
        var xslManager: PagedMediaManager? = null

        val formatters = mutableListOf<URI>()
        if (extensionAttributes.containsKey(NsCx.processor)) {
            val value = extensionAttributes[NsCx.processor]!!
            if (value.contains("/")) {
                formatters.add(URI.create(value))
            } else {
                formatters.add(URI.create("https://xmlcalabash.com/paged-media/xsl-formatter/${value}"))
            }
        }
        formatters.addAll(stepConfig.xmlCalabashConfig.pagedMediaXslProcessors)
        if (formatters.isEmpty()) {
            formatters.add(StandardPagedMediaProvider.genericXslFormatter)
        }

        for (formatter in formatters) {
            stepConfig.debug { "Searching for ${formatter} css-formatter" }
            for (manager in stepConfig.xmlCalabashConfig.pagedMediaManagers) {
                if (manager.formatterAvailable(formatter)) {
                    xslManager = manager
                    break
                }
            }
            if (xslManager != null) {
                break
            }
        }

        if (xslManager == null) {
            throw stepConfig.exception(XProcError.xdStepFailed("No XSL formatters available"))
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