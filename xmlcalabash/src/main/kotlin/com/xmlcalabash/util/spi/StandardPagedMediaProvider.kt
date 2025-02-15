package com.xmlcalabash.util.spi

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import java.net.URI

class StandardPagedMediaProvider: PagedMediaProvider, PagedMediaManager {
    companion object {
        val genericXslFormatter = URI("https://xmlcalabash.com/paged-media/xsl-formatter")
        val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
    }

    override fun formatters(): List<URI> {
        return emptyList()
    }

    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return false
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        throw RuntimeException("No provider for ${formatter}")
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return false
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        throw RuntimeException("No provider for ${formatter}")
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        throw RuntimeException("No provider for ${formatter}")
    }
}