package com.xmlcalabash.documents

import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XdmMap
import java.net.URI

interface DocumentContext {
    val baseUri: URI?
    val inscopeNamespaces: Map<String, NamespaceUri>
    val processor: Processor

    fun resolve(href: String): URI
    fun forceQNameKeys(inputMap: MapItem, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
    fun forceQNameKeys(inputMap: XdmMap, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
}