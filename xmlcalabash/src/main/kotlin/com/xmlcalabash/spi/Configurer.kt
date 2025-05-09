package com.xmlcalabash.spi

import com.xmlcalabash.XmlCalabashBuilder
import net.sf.saxon.Configuration

interface Configurer {
    fun configure(builder: XmlCalabashBuilder)
    fun configureSaxon(config: Configuration)
}