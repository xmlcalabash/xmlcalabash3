package com.xmlcalabash.util.spi

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.spi.Configurer
import net.sf.saxon.Configuration
import javax.activation.MimetypesFileTypeMap

class StandardConfigurer(): Configurer {
    override fun configure(config: XmlCalabash) {
        // nop
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun configureContentTypes(contentTypes: MutableMap<String, String>, mimeTypes: MimetypesFileTypeMap) {
        // nop
    }
}