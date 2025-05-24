package com.xmlcalabash.steps.pagedmedia.prince

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.net.URI

class PrinceManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        val princeCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/prince")
        private val pagedMediaProcessors = setOf(genericCssFormatter, princeCssFormatter)
    }

    override fun formatters(): List<URI> {
        return listOf(princeCssFormatter)
    }

    override fun create(): PagedMediaManager {
        logger.info { "Initializing PrinceXML paged media manager" }
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        CssPrince.configure(formatter, properties)
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        if (formatter !in pagedMediaProcessors) {
            return false
        }

        val exePath = CssPrince.defaultStringOptions[CssPrince._exePath]
        if (exePath == null) {
            return false
        }

        val executable = File(exePath)
        return executable.exists() && executable.canExecute()
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