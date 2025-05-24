package com.xmlcalabash.steps.pagedmedia.antennahouse

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.net.URI
import java.util.regex.Pattern

class AhManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
        val ahXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter/antenna-house")
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        val ahCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/antenna-house")

        private val pagedMediaProcessors = setOf(
            genericCssFormatter, ahCssFormatter, genericXslFormatter, ahXslFormatter
        )
    }

    override fun formatters(): List<URI> {
        return listOf(ahCssFormatter, ahXslFormatter)
    }

    override fun create(): PagedMediaManager {
        logger.info { "Initializing AntennaHouse paged media manager" }
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        if (formatter == ahCssFormatter) {
            CssAH.configure(formatter, properties)
        } else {
            FoAH.configure(formatter, properties)
        }
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        if (formatter !in pagedMediaProcessors) {
            return false
        }

        // This isn't perfect. It's possible to have all the relevant environment
        // variables defined and still not have a valid executable or something.
        // But I think it's safe to say, if you don't have the environment variables,
        // it won't work.
        val matchHome = Pattern.compile("AHF[0-9]+_HOME")
        val match64Home = Pattern.compile("AHF[0-9]+_64_HOME")
        for (env in System.getenv().keys) {
            if (matchHome.matcher(env).matches() || match64Home.matcher(env).matches()) {
                return true
            }
        }
        return false
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