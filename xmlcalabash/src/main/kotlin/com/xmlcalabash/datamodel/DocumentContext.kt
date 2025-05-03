package com.xmlcalabash.datamodel

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XPathCompiler
import net.sf.saxon.s9api.XdmNode
import java.net.URI

interface DocumentContext {
    val processor: Processor
    val location: Location
    val baseUri: URI?
    val inscopeNamespaces: Map<String, NamespaceUri>

    fun copy(): DocumentContext
    fun copy(newConfiguration: SaxonConfiguration): DocumentContext

    fun with(location: Location): DocumentContext
    fun with(prefix: String, uri: NamespaceUri): DocumentContext
    fun updateWith(node: XdmNode)
    fun updateWith(location: Location)
    fun updateWith(baseUri: URI)
    fun updateWith(prefix: String, ns: NamespaceUri)
    fun updateWith(namespaces: Map<String, NamespaceUri>)
    fun exception(error: XProcError): XProcException
    fun exception(error: XProcError, cause: Throwable): XProcException

    fun newXPathCompiler(): XPathCompiler
    fun resolve(href: String): URI
}