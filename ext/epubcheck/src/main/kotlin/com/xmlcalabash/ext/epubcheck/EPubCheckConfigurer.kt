package com.xmlcalabash.ext.epubcheck

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerProvider
import net.sf.saxon.Configuration

class EPubCheckConfigurer(): Configurer, ConfigurerProvider {
    override fun configure(builder: XmlCalabashBuilder) {
        builder.addMimeType("application/epub+zip", listOf("epub"))
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun create(): Configurer {
        return this
    }
}