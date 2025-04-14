package com.xmlcalabash.datamodel

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsXml
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XPathCompiler
import net.sf.saxon.s9api.XdmNode
import java.net.URI
import kotlin.collections.iterator

class DocumentContextImpl(private val saxonConfig: SaxonConfiguration): DocumentContext {
    private var _location: Location = Location.NULL
    private val _inscopeNamespaces = mutableMapOf<String, NamespaceUri>()
    override val processor = saxonConfig.processor

    /*
    constructor(node: XdmNode): this(node.processor) {
        updateWith(node)
    }

    constructor(processor: Processor, location: Location, nsBindings: Map<String,NamespaceUri>): this(processor) {
        _location = location
        _inscopeNamespaces.putAll(nsBindings)
    }
     */

    override val location: Location
        get() = _location

    override val baseUri: URI?
        get() = location.baseUri

    override val inscopeNamespaces: Map<String, NamespaceUri> = _inscopeNamespaces

    override fun copy(): DocumentContext {
        val context = DocumentContextImpl(saxonConfig)
        context._location = _location
        context._inscopeNamespaces.putAll(_inscopeNamespaces)
        return context
    }

    override fun with(location: Location): DocumentContext {
        val context = DocumentContextImpl(saxonConfig)
        context._location = location
        context._inscopeNamespaces.putAll(_inscopeNamespaces)
        return context
    }

    override fun with(prefix: String, uri: NamespaceUri): DocumentContext {
        val context = DocumentContextImpl(saxonConfig)
        context._location = location
        context._inscopeNamespaces.putAll(_inscopeNamespaces)
        context._inscopeNamespaces[prefix] = uri
        return context
    }

    override fun updateWith(node: XdmNode) {
        for (ns in node.axisIterator(Axis.NAMESPACE)) {
            if (node.nodeName.localName != "xml") {
                if (ns.nodeName == null) {
                    _inscopeNamespaces[""] = NamespaceUri.of(ns.stringValue)
                } else {
                    _inscopeNamespaces[ns.nodeName.localName] = NamespaceUri.of(ns.stringValue)
                }
            }
        }

        try {
            _location = Location(node)
        } catch (ex: IllegalStateException) {
            val uri = node.getAttributeValue(NsXml.base) ?: ""
            throw XProcError.xdInvalidUri(uri).exception(ex)
        }
    }

    override fun updateWith(location: Location) {
        _location = location
    }

    override fun updateWith(baseUri: URI) {
        _location = Location(baseUri, _location.lineNumber, _location.columnNumber)
    }

    override fun updateWith(prefix: String, ns: NamespaceUri) {
        _inscopeNamespaces[prefix] = ns
    }

    override fun updateWith(namespaces: Map<String, NamespaceUri>) {
        _inscopeNamespaces.putAll(namespaces)
    }

    override fun exception(error: XProcError): XProcException {
        return error.at(location).exception()
    }

    override fun exception(error: XProcError, cause: Throwable): XProcException {
        return XProcException(error.at(location), cause)
    }

    override fun newXPathCompiler(): XPathCompiler {
        val compiler = processor.newXPathCompiler()
        compiler.baseURI = baseUri
        compiler.isSchemaAware = processor.isSchemaAware
        for ((prefix, value) in inscopeNamespaces) {
            compiler.declareNamespace(prefix, value.toString())
        }
        return compiler
    }

    override fun resolve(href: String): URI {
        if (baseUri == null) {
            return URI(href)
        }
        return baseUri!!.resolve(href)
    }
}