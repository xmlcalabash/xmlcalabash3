package com.xmlcalabash.documents

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.ValueUtils
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class XProcBinaryDocument(val binaryValue: ByteArray, context: DocumentContext): XProcDocument() {
    init {
        _context = context
        val builder = SaxonTreeBuilder(context.processor)
        builder.startDocument(context.baseUri)
        builder.endDocument()
        _value = builder.result
    }

    constructor(binaryValue: ByteArray, context: DocumentContext, initialProperties: DocumentProperties): this(binaryValue, context) {
        _properties.setAll(initialProperties)
    }

    override fun with(newValue: ByteArray): XProcDocument {
        return XProcBinaryDocument(newValue, context, _properties)
    }

    override fun with(newValue: XdmValue): XProcDocument {
        val newProps = DocumentProperties(_properties)
        newProps[Ns.contentType] = ValueUtils.contentClassification(newValue)
        newProps.remove(Ns.baseUri)
        if (newValue is XdmNode) {
            newProps[Ns.baseUri] = newValue.baseURI
        }
        return XProcDocument(newValue, context, newProps)
    }

    override fun with(newValue: ByteArray, baseURI: URI?): XProcDocument {
        val bin = XProcBinaryDocument(newValue, context, _properties)
        if (baseURI != null) {
            bin.properties.remove(Ns.baseUri)
        }
        bin.properties[Ns.baseUri] = baseURI
        return bin
    }

    override fun with(newValue: XdmValue, baseURI: URI?): XProcDocument {
        val newProps = DocumentProperties(_properties)
        newProps[Ns.contentType] = ValueUtils.contentClassification(newValue)
        newProps.remove(Ns.baseUri)
        newProps[Ns.baseUri] = baseURI
        return XProcDocument(newValue, context, newProps)
    }

    override fun with(contentType: MediaType, removeSerialization: Boolean): XProcDocument {
        if (_properties.has(Ns.contentType)
            || (removeSerialization && _properties.has(Ns.serialization))) {
            val newProps = DocumentProperties(_properties)
            newProps[Ns.contentType] = contentType
            if (removeSerialization) {
                newProps.remove(Ns.serialization)
            }
            return XProcBinaryDocument(binaryValue, context, newProps)
        }
        val bin = XProcBinaryDocument(binaryValue, context, _properties)
        bin.properties[Ns.contentType] = contentType
        return bin
    }

    override fun with(properties: DocumentProperties): XProcDocument {
        return XProcBinaryDocument(binaryValue, context, properties)
    }
}