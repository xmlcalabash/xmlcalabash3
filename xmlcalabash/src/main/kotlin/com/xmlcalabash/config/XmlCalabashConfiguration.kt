package com.xmlcalabash.config

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.util.SchematronAssertions
import com.xmlcalabash.util.Verbosity
import com.xmlcalabash.visualizers.Silent
import net.sf.saxon.Configuration
import net.sf.saxon.s9api.QName
import java.io.File
import java.net.URI

abstract class XmlCalabashConfiguration {
    abstract fun saxonConfigurer(saxon: Configuration)
    abstract fun xmlCalabashConfigurer(xmlCalabash: XmlCalabash)

    var verbosity = Verbosity.INFO
    var visualizer: Monitor = Silent(emptyMap())
    var messageBufferSize = 32
    var assertions = SchematronAssertions.WARNING
    var saxonConfigurationFile: File? = null
    var saxonConfigurationProperties: Map<String,String> = emptyMap()
    var uniqueInlineUris: Boolean = true
    var licensed: Boolean = true // If a licensed configuration can't be loaded, an unlicensed one will be...
    var proxies: Map<String, String> = emptyMap()
    var inlineTrimWhitespace: Boolean = false
    var mpt: Double = 0.99999998
    var graphviz: File? = null
    var serialization: Map<MediaType, Map<QName, String>> = emptyMap()
    var trace: File? = null
    var traceDocuments: File? = null
    var debugger = false
    var mimetypes: Map<String, String> = emptyMap()
    var sendmail: Map<String, String> = emptyMap()
    var pagedMediaManagers: List<PagedMediaManager> = emptyList()
    var pagedMediaXslProcessors: List<URI> = emptyList()
    var pagedMediaCssProcessors: List<URI> = emptyList()
    var threadPoolSize: Int = 1
}