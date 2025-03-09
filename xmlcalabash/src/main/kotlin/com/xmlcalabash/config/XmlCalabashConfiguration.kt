package com.xmlcalabash.config

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.DefaultMessagePrinter
import com.xmlcalabash.util.Verbosity
import com.xmlcalabash.visualizers.Silent
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import net.sf.saxon.s9api.XdmNode
import java.io.File
import java.net.URI

abstract class XmlCalabashConfiguration {
    companion object {
        val OS = System.getProperty("os.name") ?: "unknown"
        val DEFAULT_CONSOLE_ENCODING = if (OS.startsWith("Windows")) {
            "windows-1252"
        } else {
            "utf-8"
        }
    }

    var verbosity = Verbosity.INFO
    var visualizer: Monitor = Silent(emptyMap())
    var messageBufferSize = 32
    var assertions = AssertionsLevel.WARNING
    var saxonConfigurationFile: File? = null
    var saxonConfigurationProperties: Map<String,String> = emptyMap()
    var uniqueInlineUris: Boolean = true
    var licensed: Boolean = true // If a licensed configuration can't be loaded, an unlicensed one will be...
    val xmlSchemaDocuments = mutableListOf<XdmNode>()
    var validationMode = ValidationMode.DEFAULT
    var tryNamespaces = false
    var useLocationHints = false
    val xmlCatalogs = mutableListOf<URI>()
    var proxies: Map<String, String> = emptyMap()
    var inlineTrimWhitespace: Boolean = false
    var mpt: Double = 0.99999998
    var graphviz: File? = null
    var serialization: Map<MediaType, Map<QName, String>> = emptyMap()
    var trace: File? = null
    var traceDocuments: File? = null
    var debugger = false
    var pipe = false
    var consoleEncoding = DEFAULT_CONSOLE_ENCODING
    var messagePrinter: MessagePrinter = DefaultMessagePrinter(DEFAULT_CONSOLE_ENCODING)
    var mimetypes: Map<String, String> = emptyMap()
    var sendmail: Map<String, String> = emptyMap()
    var pagedMediaManagers: List<PagedMediaManager> = emptyList()
    var pagedMediaXslProcessors: List<URI> = emptyList()
    var pagedMediaCssProcessors: List<URI> = emptyList()
    var threadPoolSize: Int = 1
    var other = mutableMapOf<QName, List<Map<QName, String>>>()
}