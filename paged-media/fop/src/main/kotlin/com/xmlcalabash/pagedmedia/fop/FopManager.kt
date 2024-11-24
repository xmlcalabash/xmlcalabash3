package com.xmlcalabash.pagedmedia.fop

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import java.net.URI

class FopManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
        private val fopXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter/fop")
        private val pagedMediaProcessors = setOf(genericXslFormatter, fopXslFormatter)
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        throw RuntimeException("paged-media-fop does not provide any CSS formatters")
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        when (formatter) {
            genericXslFormatter, fopXslFormatter -> return FoFop()
            else -> throw RuntimeException("paged-media-fop does not provide ${formatter}")
        }
    }
}