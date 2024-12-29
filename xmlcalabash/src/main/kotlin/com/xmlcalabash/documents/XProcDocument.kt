package com.xmlcalabash.documents

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.ValueUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.EmptySequence
import net.sf.saxon.value.QNameValue
import java.io.OutputStream
import java.net.URI

open class XProcDocument internal constructor() {
    val id = ++_id
    private var _valueFunction: () -> XdmValue = { -> XdmEmptySequence.getInstance() }
    protected var _value: XdmValue? = null
    protected lateinit var _context: DocumentContext
    protected val _properties = DocumentProperties()

    internal constructor(value: XdmValue, context: DocumentContext): this(value, context, DocumentProperties())
    internal constructor(value: XdmValue, context: DocumentContext, initialProperties: DocumentProperties): this() {
        _value = value
        _context = context
        _properties.setAll(initialProperties)
        adjustValue()
        if (_properties.isEmpty() && context.baseUri != null) {
            _properties[Ns.baseUri] = context.baseUri
        }
        if (_properties.contentType == null) {
            _properties[Ns.contentType] = ValueUtils.contentClassification(_value!!)
        }
    }

    constructor(valueFunction: () -> XdmValue, context: DocumentContext): this() {
        _valueFunction = valueFunction
        _context = context
    }

    constructor(valueFunction: () -> XdmValue, context: DocumentContext, initialProperties: DocumentProperties): this() {
        _valueFunction = valueFunction
        _context = context
        initialProperties.setAll(initialProperties)
    }

    private fun evaluate(): XdmValue {
        synchronized(this) {
            if (_value == null) {
                _value = _valueFunction()
                adjustValue()
                if (_properties.baseURI == null) {
                    if (_value is XdmNode) {
                        if (context.baseUri == null) {
                            _properties[Ns.baseUri] = (_value as XdmNode).baseURI
                        } else {
                            _properties[Ns.baseUri] = context.baseUri!!.resolve((_value as XdmNode).baseURI)
                        }
                    } else {
                        _properties[Ns.baseUri] = context.baseUri
                    }
                }
                if (_properties.contentType == null) {
                    _properties[Ns.contentType] = ValueUtils.contentClassification(_value!!)
                }
            }
            return _value!!
        }
    }

    private fun adjustValue() {
        if (contentType != null && !contentType!!.xmlContentType() && !contentType!!.textContentType()) {
            // Leave it alone
            return
        }

        // If the value is an atomic string value, make it a document containing a text node
        if (_value is XdmAtomicValue && (_value as XdmAtomicValue).primitiveTypeName == NsXs.string) {
            val baseURI = properties.getUri(Ns.baseUri)
            val builder = SaxonTreeBuilder(context.processor)
            builder.startDocument(baseURI)
            builder.addSubtree(value)
            builder.endDocument()
            _value = builder.result
        }
    }

    val value: XdmValue
        get() = _value ?: evaluate()

    val properties: DocumentProperties
        get() {
            if (_value == null) {
                evaluate()
            }
            return _properties
        }

    val context: DocumentContext
        get() = _context

    companion object {
        private var _id = 0L

        fun ofEmpty(context: DocumentContext): XProcDocument {
            return ofEmpty(context, DocumentProperties())
        }
        fun ofEmpty(context: DocumentContext, properties: DocumentProperties): XProcDocument {
            return XProcDocument(XdmAtomicValue.wrap(EmptySequence.getInstance()), context, withDefaults(properties, context.baseUri, MediaType.OCTET_STREAM))
        }
        fun ofJson(value: XdmValue, context: DocumentContext): XProcDocument {
            return ofJson(value, context, MediaType.JSON, DocumentProperties())
        }
        fun ofJson(value: XdmValue, context: DocumentContext, contentType: MediaType = MediaType.JSON, properties: DocumentProperties): XProcDocument {
            return XProcDocument(value, context, withDefaults(properties, context.baseUri, contentType))
        }
        fun ofText(value: XdmValue, context: DocumentContext, contentType: MediaType = MediaType.TEXT, properties: DocumentProperties): XProcDocument {
            val node = if (value is XdmNode) {
                value
            } else {
                val baseURI = properties.getUri(Ns.baseUri)
                val builder = SaxonTreeBuilder(context.processor)
                builder.startDocument(baseURI)
                builder.addSubtree(value)
                builder.endDocument()
                builder.result
            }

            // Patch the content type
            val pprop = DocumentProperties(properties)
            pprop[Ns.contentType] = contentType

            return XProcDocument(node, context, withDefaults(pprop, node.baseURI, contentType))
        }
        fun ofText(value: String, context: DocumentContext, contentType: MediaType = MediaType.TEXT, properties: DocumentProperties): XProcDocument {
            return ofText(XdmAtomicValue(value), context, contentType, properties)
        }
        fun ofText(value: XdmNode, context: DocumentContext): XProcDocument {
            return ofText(value, context, MediaType.TEXT, DocumentProperties())
        }
        fun ofText(value: String, context: DocumentContext): XProcDocument {
            return ofText(XdmAtomicValue(value), context, MediaType.TEXT, DocumentProperties())
        }
        fun ofBinary(value: ByteArray, context: DocumentContext, contentType: MediaType, properties: DocumentProperties): XProcDocument {
            return XProcBinaryDocument(value, context, withDefaults(properties, context.baseUri, contentType))
        }
        fun ofBinary(value: ByteArray, context: DocumentContext, properties: DocumentProperties): XProcDocument {
            return ofBinary(value, context, MediaType.OCTET_STREAM, properties)
        }
        fun ofXml(value: XdmNode, context: DocumentContext, contentType: MediaType): XProcDocument {
            return ofXml(value, context, contentType, DocumentProperties())
        }
        fun ofXml(value: XdmNode, context: DocumentContext, contentType: MediaType, properties: DocumentProperties): XProcDocument {
            val baseURI = value.baseURI ?: context.baseUri
            return XProcDocument(value, context, withDefaults(properties, baseURI, contentType))
        }
        fun ofXml(value: XdmNode, context: DocumentContext): XProcDocument {
            return ofXml(value, context, MediaType.XML, DocumentProperties())
        }
        fun ofXml(value: XdmNode, context: DocumentContext, properties: DocumentProperties): XProcDocument {
            return ofXml(value, context, MediaType.XML, properties)
        }
        fun ofValue(value: XdmValue, context: DocumentContext, contentType: MediaType? = null, properties: DocumentProperties = DocumentProperties()): XProcDocument {
            if (value is XdmNode) {
                return ofXml(value, context, withDefaults(properties, value.baseURI, contentType))
            }
            return XProcDocument(value, context, withDefaults(properties, context.baseUri, contentType))
        }
        private fun withDefaults(properties: DocumentProperties, baseURI: URI?, contentType: MediaType? = null): DocumentProperties {
            if ((properties.has(Ns.baseUri) || baseURI == null)
                && (properties.has(Ns.contentType) || contentType == null)){
                return properties
            }
            val props = DocumentProperties(properties)
            if (!properties.has(Ns.baseUri)) {
                props[Ns.baseUri] = baseURI
            }
            if (!properties.has(Ns.contentType)) {
                props[Ns.contentType] = contentType
            }
            return props
        }
    }

    val baseURI: URI?
        get() {
            return _properties.baseURI
        }

    val contentType: MediaType?
        get()  {
            return _properties.contentType
        }

    val inScopeNamespaces: Map<String,NamespaceUri>
        get() {
            return context.inscopeNamespaces
        }

    open fun with(newValue: ByteArray): XProcDocument {
        if (_properties.contentClassification != MediaType.OCTET_STREAM) {
            val newProps = DocumentProperties(_properties)
            newProps[Ns.contentType] = MediaType.OCTET_STREAM
            return XProcBinaryDocument(newValue, context, newProps)
        }
        return XProcBinaryDocument(newValue, context, _properties)
    }

    open fun with(newValue: XdmValue): XProcDocument {
        // If the document is now just text, update the media type
        if (ValueUtils.contentClassification(newValue) == MediaType.TEXT && _properties.contentClassification != MediaType.TEXT) {
            val newProps = DocumentProperties(_properties)
            newProps[Ns.contentType] = MediaType.TEXT
            return XProcDocument(newValue, context, newProps)
        }
        return XProcDocument(newValue, context, _properties)
    }

    open fun with(newValue: ByteArray, baseURI: URI?): XProcDocument {
        val newProps = DocumentProperties(_properties)
        if (_properties.contentClassification != MediaType.OCTET_STREAM) {
            newProps[Ns.contentType] = MediaType.OCTET_STREAM
        }
        if (baseURI == null) {
            newProps.remove(Ns.baseUri)
        }
        newProps[Ns.baseUri] = baseURI
        return XProcBinaryDocument(newValue, context, newProps)
    }

    open fun with(newValue: XdmValue, baseURI: URI?): XProcDocument {
        val newProps = DocumentProperties(_properties)
        if (baseURI == null) {
            newProps.remove(Ns.baseUri)
        }
        newProps[Ns.baseUri] = baseURI

        if (newValue is XdmNode && baseURI != null && baseURI != newValue.baseURI) {
            val builder = SaxonTreeBuilder(newValue.processor)
            builder.startDocument(baseURI)
            builder.addSubtree(newValue)
            builder.endDocument()
            return XProcDocument(builder.result, context, newProps)
        }

        return XProcDocument(newValue, context, newProps)
    }

    open fun with(contentType: MediaType, removeSerialization: Boolean = false): XProcDocument {
        if (_properties.has(Ns.contentType)
            || (removeSerialization && _properties.has(Ns.serialization))) {
            val newProps = DocumentProperties(_properties)
            newProps[Ns.contentType] = contentType
            if (removeSerialization) {
                newProps.remove(Ns.serialization)
            }
            return XProcDocument(value, context, newProps)
        }
        return ofValue(value, context, contentType, _properties)
    }

    open fun with(properties: DocumentProperties): XProcDocument {
        val baseURI = if (properties.has(Ns.baseUri)) {
            URI(properties[Ns.baseUri].toString())
        } else {
            null
        }

        // If the properties change the base URI, make sure we update the actual document base URI if we can
        if (value is XdmNode && baseURI != (value as XdmNode).baseURI) {
            val builder = SaxonTreeBuilder((value as XdmNode).processor)
            builder.startDocument(baseURI)
            builder.addSubtree(value)
            builder.endDocument()
            return XProcDocument(builder.result, context, properties)
        }

        val doc = XProcDocument(value, context, properties)
        return doc
    }

    fun serialize(defProp: Map<QName, XdmValue> = emptyMap()) {
        serialize(System.out, defProp)
    }

    fun serialize(out: OutputStream, defProp: Map<QName, XdmValue> = emptyMap()) {
        val ctype = contentType ?: MediaType.OCTET_STREAM
        val serializer = XProcSerializer(context)
        serializer.setDefaultProperties(ctype, defProp)
        serializer.write(this, out, "output stream")
    }

    override fun toString(): String {
        return if (contentType != null) {
            "${contentType} document"
        } else {
            "application/octet-stream document"
        }
    }
}