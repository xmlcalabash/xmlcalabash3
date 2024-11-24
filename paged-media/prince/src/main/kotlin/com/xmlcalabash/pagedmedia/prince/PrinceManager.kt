package com.xmlcalabash.pagedmedia.prince

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import java.net.URI

class PrinceManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        private val princeCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/prince")
        private val pagedMediaProcessors = setOf(genericCssFormatter, princeCssFormatter)
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        when (formatter) {
            genericCssFormatter, princeCssFormatter -> return CssPrince()
            else -> throw RuntimeException("paged-media-prince does not provide ${formatter}")
        }
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        throw RuntimeException("paged-media-prince does not provide any XSL formatters")
    }
}