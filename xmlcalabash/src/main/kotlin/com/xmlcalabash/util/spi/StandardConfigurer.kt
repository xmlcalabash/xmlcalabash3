package com.xmlcalabash.util.spi

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.spi.Configurer
import net.sf.saxon.Configuration

class StandardConfigurer(): Configurer {
    override fun configure(builder: XmlCalabashBuilder) {
        // nop
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }
}