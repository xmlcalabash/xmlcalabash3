package com.xmlcalabash.spi

import com.xmlcalabash.XmlCalabash
import net.sf.saxon.Configuration
import javax.activation.MimetypesFileTypeMap

interface Configurer {
    fun configure(xmlcalabash: XmlCalabash)
    fun configureSaxon(config: Configuration)
    fun configureContentTypes(contentTypes: MutableMap<String, String>, mimeTypes: MimetypesFileTypeMap)
}