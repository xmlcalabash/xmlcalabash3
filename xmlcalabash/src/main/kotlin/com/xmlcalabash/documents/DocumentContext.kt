package com.xmlcalabash.documents

import com.xmlcalabash.runtime.ValueConverter
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import java.net.URI

interface DocumentContext: ValueConverter {
    val baseUri: URI?
    val inscopeNamespaces: Map<String, NamespaceUri>
    val processor: Processor
    fun resolve(href: String): URI
}