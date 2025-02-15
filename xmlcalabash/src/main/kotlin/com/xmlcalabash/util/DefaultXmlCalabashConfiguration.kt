package com.xmlcalabash.util

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.config.XmlCalabashConfiguration
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import net.sf.saxon.Configuration

class DefaultXmlCalabashConfiguration(): XmlCalabashConfiguration() {
    init {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            val manager = provider.create()
            managers.add(manager)
            for (formatter in manager.formatters()) {
                manager.configure(formatter, emptyMap())
            }
        }
        pagedMediaManagers = managers
    }
}