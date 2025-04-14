package com.xmlcalabash

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import java.io.File
import java.net.URI

interface XmlCalabashConfiguration {
    val saxonConfiguration: SaxonConfiguration
    val assertions: AssertionsLevel
    val consoleEncoding: String
    val debug: Boolean
    val debugger: Boolean
    val eagerEvaluation: Boolean
    val graphStyle: URI?
    val graphviz: File?
    val inlineTrimWhitespace: Boolean
    val licensed: Boolean
    val messageBufferSize: Int
    val messagePrinter: MessagePrinter
    val messageReporter: MessageReporter
    val mimetypes: Map<String, List<String>>
    val other: Map<QName, List<Map<QName, String>>>
    val pagedMediaCssProcessors: List<URI>
    val pagedMediaManagers: List<PagedMediaManager>
    val pagedMediaXslProcessors: List<URI>
    val pipe: Boolean
    val proxies: Map<String, String>
    val saxonConfigurationFile: File?
    val saxonConfigurationProperties: Map<String,String>
    val sendmail: Map<String, String>
    val serialization: Map<MediaType, Map<QName,String>>
    val threadPoolSize: Int
    val trace: File?
    val traceDocuments: File?
    val tryNamespaces: Boolean
    val uniqueInlineUris: Boolean
    val useLocationHints: Boolean
    val validationMode: ValidationMode
    val verbosity: Verbosity
    val visualizer: String
    val visualizerProperties: Map<String,String>
    val xmlCatalogs: List<URI>
    val xmlSchemaDocuments: List<URI>
    val initializerClasses: List<Pair<String,Boolean>>
    val configurers: List<Configurer>
}