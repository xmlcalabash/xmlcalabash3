package com.xmlcalabash.pagedmedia.weasyprint

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import java.io.File
import java.net.URI

class WeasyprintManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        val weasyprintCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/weasyprint")
        private val pagedMediaProcessors = setOf(genericCssFormatter, weasyprintCssFormatter)
    }

    override fun formatters(): List<URI> {
        return listOf(weasyprintCssFormatter)
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        CssWeasyprint.configure(formatter, properties)
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        if (formatter !in pagedMediaProcessors) {
            return false
        }
        val exePath = CssWeasyprint.defaultStringOptions[CssWeasyprint._exePath]
        if (exePath == null) {
            return false
        }

        val exeName = File(exePath)
        return exeName.exists() && exeName.canExecute()
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        when (formatter) {
            genericCssFormatter, weasyprintCssFormatter -> return CssWeasyprint()
            else -> throw RuntimeException("paged-media-weasyprint does not provide ${formatter}")
        }
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        throw RuntimeException("paged-media-weasyprint does not provide any XSL formatters")
    }
}