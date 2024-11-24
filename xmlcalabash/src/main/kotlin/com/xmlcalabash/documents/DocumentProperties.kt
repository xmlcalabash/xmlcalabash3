package com.xmlcalabash.documents

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class DocumentProperties() {
    private var properties = mutableMapOf<QName, XdmValue>()

    constructor(docprop: DocumentProperties) : this() {
        properties.putAll(docprop.properties)
    }

    constructor(docprop: Map<QName,XdmValue>) : this() {
        properties.putAll(docprop)
    }

    fun isEmpty(): Boolean = properties.isEmpty()
    fun isNotEmpty(): Boolean = !isEmpty()

    operator fun set(name: QName, value: XdmValue?) {
        if (value != null) {
            properties[name] = value
        }
    }

    operator fun set(name: QName, newType: MediaType?) {
        if (newType == null) {
            return
        }
        val curtype = contentType
        if (name == Ns.contentType && curtype != null && curtype.classification() != newType.classification()) {
            properties.remove(Ns.serialization)
        }
        properties[name] = XdmAtomicValue(newType.toString())
    }

    operator fun set(name: QName, uri: URI?) {
        if (uri != null) {
            properties[name] = XdmAtomicValue(uri)
        }
    }

    operator fun set(name: QName, value: String?) {
        if (value != null) {
            properties[name] = XdmAtomicValue(value)
        }
    }

    operator fun set(name: QName, value: Boolean?) {
        if (value != null) {
            properties[name] = XdmAtomicValue(value)
        }
    }

    fun setAll(newProperties: DocumentProperties) {
        properties.clear()
        properties.putAll(newProperties.properties)
    }

    fun setAll(newProperties: Map<QName,XdmValue>) {
        properties.clear()
        properties.putAll(newProperties)
    }

    operator fun get(name: QName): XdmValue? {
        return properties[name]
    }

    fun getMediaType(name: QName): MediaType? {
        if (properties.containsKey(name)) {
            return MediaType.parse(properties[name]!!.underlyingValue.stringValue)
        }
        return null
    }

    fun getUri(name: QName): URI? {
        if (properties.containsKey(name)) {
            return URI(properties[name]!!.underlyingValue.stringValue)
        }
        return null
    }

    fun getString(name: QName): String? {
        return properties[name]?.underlyingValue?.stringValue
    }

    fun asMap(): Map<QName, XdmValue> {
        return properties
    }

    fun has(QName: QName): Boolean {
        return properties.containsKey(QName)
    }

    fun remove(name: QName) {
        properties.remove(name)
    }

    val baseURI: URI?
        get() {
            if (properties.containsKey(Ns.baseUri)) {
                // FIXME: get directly from the underlying value?
                return URI(properties[Ns.baseUri]!!.underlyingValue.stringValue)
            }
            return null
        }

    val contentType: MediaType?
        get()  {
            if (properties.containsKey(Ns.contentType)) {
                var ctype = MediaType.parse(properties[Ns.contentType]!!.underlyingValue.stringValue)
                val serial = properties[Ns.serialization]
                if (serial != null) {
                    val encoding = (serial as XdmMap).get(XdmAtomicValue(Ns.encoding))
                    if (encoding != null) {
                        ctype = ctype.addParam("charset", encoding.underlyingValue.stringValue)
                    }
                }
                return ctype
            }
            return null
        }

    val contentClassification: MediaType
        get() {
            return contentType?.classification() ?: MediaType.OCTET_STREAM
        }
}