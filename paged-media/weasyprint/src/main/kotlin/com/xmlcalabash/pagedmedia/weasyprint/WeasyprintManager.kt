package com.xmlcalabash.pagedmedia.weasyprint

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import java.net.URI

class WeasyprintManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        private val weasyprintCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/weasyprint")
        private val pagedMediaProcessors = setOf(genericCssFormatter, weasyprintCssFormatter)
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
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