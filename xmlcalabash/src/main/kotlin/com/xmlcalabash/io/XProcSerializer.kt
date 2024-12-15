package com.xmlcalabash.io

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.*
import net.sf.saxon.value.QNameValue
import java.io.OutputStream

class XProcSerializer(val processor: Processor) {
    constructor(config: XProcStepConfiguration): this(config.processor)

    val overrideProperties = mutableMapOf<QName, String>()

    fun write(doc: XProcDocument, stream: OutputStream, overrideContentType: MediaType? = null,
              defaultProperties: Map<QName,XdmValue> = mapOf()) {
        val contentType =  overrideContentType ?: doc.contentType ?: MediaType.OCTET_STREAM

        if (contentType.htmlContentType()) {
            if (contentType == MediaType.XHTML) {
                serializeXml(doc, stream, defaultProperties)
            } else {
                serializeHtml(doc, stream, defaultProperties)
            }
            return
        }

        if (contentType.xmlContentType() ) {
            serializeXml(doc, stream, defaultProperties)
            return
        }

        if (contentType.jsonContentType()) {
            serializeJson(doc, stream, defaultProperties)
            return
        }

        if (contentType.textContentType()) {
            serializeText(doc, stream, defaultProperties)
            return
        }

        if (doc is XProcBinaryDocument) {
            stream.write(doc.binaryValue)
            return
        }

        if (doc.value is XdmNode) {
            serializeXml(doc, stream, defaultProperties)
        } else {
            serializeJson(doc, stream, defaultProperties)
        }
    }

    fun write(node: XdmNode, stream: OutputStream, defaultProperties: Map<QName, XdmValue> = mapOf()) {
        if (S9Api.isTextDocument(node)) {
            serializeText(node, stream, defaultProperties)
        } else {
            serializeXml(node, stream, defaultProperties)
        }
    }

    private fun serializeXml(doc: XProcDocument, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, doc, "xml", defaultProperties)
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeXml(node: XdmNode, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer,"xml", defaultProperties)
        serializer.serializeXdmValue(node)
    }

    private fun serializeHtml(doc: XProcDocument, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, doc, "html", defaultProperties)
        serializer.setOutputProperty(Serializer.Property.HTML_VERSION, "5")
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeJson(doc: XProcDocument, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, doc, "json", defaultProperties)
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeText(doc: XProcDocument, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, doc, "text", defaultProperties)
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeText(node: XdmNode, stream: OutputStream, defaultProperties: Map<QName, XdmValue>) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer,"text", defaultProperties)
        serializer.serializeXdmValue(node)
    }

    private fun setSerializationProperties(serializer: Serializer, doc: XProcDocument, method: String, defaultProperties: Map<QName, XdmValue>) {
        try {
            for ((name, value) in defaultProperties) {
                serializer.setOutputProperty(name, value.underlyingValue.stringValue)
            }

            val props = doc.properties[Ns.serialization]
            if (props != null) {
                val map = doc.context.forceQNameKeys(props as XdmMap, doc.context.inscopeNamespaces)
                for (key in map.keySet()) {
                    val keyvalue = key.underlyingValue as QNameValue
                    val name = QName(keyvalue.namespaceURI, keyvalue.localName)
                    val value = map.get(key)
                    serializer.setOutputProperty(name, value.underlyingValue.stringValue)
                }
            }

            for ((name, value) in overrideProperties) {
                serializer.setOutputProperty(name, value)
            }

            serializer.setOutputProperty(Serializer.Property.METHOD, method)
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }

    private fun setSerializationProperties(serializer: Serializer, method: String, defaultProperties: Map<QName, XdmValue>) {
        try {
            for ((name, value) in defaultProperties) {
                serializer.setOutputProperty(name, value.underlyingValue.stringValue)
            }
            for ((name, value) in overrideProperties) {
                serializer.setOutputProperty(name, value)
            }

            serializer.setOutputProperty(Serializer.Property.METHOD, method)
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }
}