package com.xmlcalabash.util

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.config.XmlCalabashConfiguration
import net.sf.saxon.Configuration

class DefaultXmlCalabashConfiguration(): XmlCalabashConfiguration() {
    override fun saxonConfigurer(saxon: Configuration) {
        // nop
    }

    override fun xmlCalabashConfigurer(xmlCalabash: XmlCalabash) {
        // nop
    }
}