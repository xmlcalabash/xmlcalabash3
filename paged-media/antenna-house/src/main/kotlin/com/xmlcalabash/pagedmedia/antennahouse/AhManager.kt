package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import java.net.URI

class AhManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
        private val ahXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter/antenna-house")
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        private val ahCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/antenna-house")

        private val pagedMediaProcessors = setOf(
            genericCssFormatter, ahCssFormatter, genericXslFormatter, ahXslFormatter
        )
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        when (formatter) {
            genericCssFormatter, ahCssFormatter -> return CssAH()
            else -> throw RuntimeException("paged-media-antenna-house does not provide ${formatter}")
        }
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        when (formatter) {
            genericXslFormatter, ahXslFormatter -> return FoAH()
            else -> throw RuntimeException("paged-media-antenna-house does not provide ${formatter}")
        }
    }
}