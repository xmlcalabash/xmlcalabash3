package com.xmlcalabash.pagedmedia.fop

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.net.URI

class FopManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
        val fopXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter/fop")
        private val pagedMediaProcessors = setOf(genericXslFormatter, fopXslFormatter)
    }

    override fun formatters(): List<URI> {
        return listOf(fopXslFormatter)
    }

    override fun create(): PagedMediaManager {
        logger.info { "Initializing Apache FOP paged media manager" }
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        FoFop.configure(formatter, properties)
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