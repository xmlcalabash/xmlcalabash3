package com.xmlcalabash.documents

import com.xmlcalabash.config.XmlCalabash
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import java.net.URI

interface DocumentContext {
    val xmlCalabash: XmlCalabash
    val baseUri: URI?
    val inscopeNamespaces: Map<String, NamespaceUri>
    val processor: Processor

    fun resolve(href: String): URI
    fun parseQName(name: String): QName
    fun parseNCName(name: String): String
    fun forceQNameKeys(inputMap: MapItem, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
    fun forceQNameKeys(inputMap: XdmMap, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
}