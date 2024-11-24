package com.xmlcalabash.config

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.Configuration
import net.sf.saxon.s9api.QName
import java.io.File

abstract class XmlCalabashConfiguration {
    abstract fun saxonConfigurer(saxon: Configuration)
    abstract fun xmlCalabashConfigurer(xmlCalabash: XmlCalabash)

    var debug = false
    var verbosity = Verbosity.NORMAL
    var saxonConfigurationFile: File? = null
    var saxonConfigurationProperties: Map<String,String> = emptyMap()
    var uniqueInlineUris: Boolean = true
    var schemaAware: Boolean = false
    var proxies: Map<String, String> = emptyMap()
    var inlineTrimWhitespace: Boolean = false
    var mpt: Double = 0.99999998
    var graphviz: File? = null
    var serialization: Map<MediaType, Map<QName, String>> = emptyMap()
    var mimetypes: Map<String, String> = emptyMap()
    var sendmail: Map<String, String> = emptyMap()
    var threadPoolSize: Int = 1
}