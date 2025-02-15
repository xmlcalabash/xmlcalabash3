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

    override fun run() {
        super.run()
        document = queues["source"]!!.first()
        stylesheets.addAll(queues["stylesheet"]!!)

        val contentType = mediaTypeBinding(Ns.contentType, MediaType.PDF)
        val parameters = qnameMapBinding(Ns.parameters)
        var cssManager: PagedMediaManager? = null

        val formatters = mutableListOf<URI>()
        if (extensionAttributes.containsKey(NsCx.processor)) {
            val value = extensionAttributes[NsCx.processor]!!
            if (value.contains("/")) {
                formatters.add(URI.create(value))
            } else {
                formatters.add(URI.create("https://xmlcalabash.com/paged-media/css-formatter/${value}"))
            }
        }
        formatters.addAll(stepConfig.xmlCalabash.xmlCalabashConfig.pagedMediaCssProcessors)
        if (formatters.isEmpty()) {
            formatters.add(StandardPagedMediaProvider.genericCssFormatter)
        }

        for (formatter in formatters) {
            stepConfig.debug { "Searching for ${formatter} css-formatter" }
            for (manager in stepConfig.xmlCalabash.xmlCalabashConfig.pagedMediaManagers) {
                if (manager.formatterAvailable(formatter)) {
                    cssManager = manager
                    break
                }
            }
            if (cssManager != null) {
                break
            }
        }

        if (cssManager == null) {
            throw stepConfig.exception(XProcError.xdStepFailed("No CSS formatters available"))
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