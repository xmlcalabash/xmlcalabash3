package com.xmlcalabash.io

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.*
import net.sf.saxon.value.QNameValue
import org.apache.logging.log4j.kotlin.logger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class XProcSerializer(val xmlCalabash: XmlCalabash, val processor: Processor) {
    constructor(config: XProcStepConfiguration): this(config.xmlCalabash, config.processor)
    constructor(context: DocumentContext): this(context.xmlCalabash, context.processor)

    private val _defaultProperties = mutableMapOf<MediaType, MutableMap<QName, String>>()
    private val _overrideProperties = mutableMapOf<MediaType, MutableMap<QName, String>>()
    private val properties = mutableMapOf<QName, String>()

    val defaultProperties: Map<MediaType, Map<QName, String>> get() = _defaultProperties
    val overrideProperties: Map<MediaType, Map<QName, String>> get() = _overrideProperties

    init {
        for ((ctype, props) in xmlCalabash.xmlCalabashConfig.serialization) {
            _defaultProperties[ctype] = mutableMapOf()
            _defaultProperties[ctype]!!.putAll(props)
        }
    }

    fun setDefaultProperty(contentType: MediaType, name: QName, value: String) {
        val props = _defaultProperties[contentType] ?: mutableMapOf()
        props[name] = value
        _defaultProperties[contentType] = props
    }

    fun setDefaultProperties(contentType: MediaType, props: Map<QName, XdmValue>) {
        val props = combineProperties(_defaultProperties[contentType] ?: mapOf(), props)
        _defaultProperties[contentType] = props
    }

    fun setOverrideProperty(contentType: MediaType, name: QName, value: String) {
        val props = _overrideProperties[contentType] ?: mutableMapOf()
        props[name] = value
        _overrideProperties[contentType] = props
    }

    fun setOverrideProperties(contentType: MediaType, props: Map<QName, XdmValue>) {
        val props = combineProperties(_overrideProperties[contentType] ?: mapOf(), props)
        _overrideProperties[contentType] = props
    }

    private fun combineProperties(initialProperties: Map<QName,String>, docprops: Map<QName, XdmValue>): MutableMap<QName, String> {
        val props = mutableMapOf<QName, String>()
        props.putAll(initialProperties)

        for ((key, value) in docprops) {
            if (value.underlyingValue is QNameValue) {
                val qname = (value.underlyingValue as QNameValue)
                props[key] = "Q{${qname.namespaceURI}}${qname.localName}"
            } else {
                props[key] = value.underlyingValue.stringValue
            }
        }

        return props
    }

    fun write(doc: XProcDocument, file: File, overrideContentType: MediaType? = null) {
        val stream = FileOutputStream(file)
        write(doc, stream, file.absolutePath, overrideContentType)
        stream.close()
    }

    fun write(doc: XProcDocument, stream: OutputStream, purpose: String, overrideContentType: MediaType? = null) {
        if (doc is XProcBinaryDocument) {
            logger.debug { "Serializing binary output for ${purpose}" }
            stream.write(doc.binaryValue)
            return
        }

        val contentType =  overrideContentType ?: doc.contentType ?: MediaType.OCTET_STREAM

        properties.clear()
        properties.putAll(defaultProperties[contentType] ?: mapOf())

        val docSerMap = doc.properties[Ns.serialization]
        if (docSerMap != null) {
            val docProp = mutableMapOf<QName, XdmValue>()
            for (key in (docSerMap as XdmMap).keySet()) {
                val value = docSerMap[key]
                val qvalue = key.underlyingValue
                val qkey = if (qvalue is QNameValue) {
                    QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
                } else {
                    throw RuntimeException("Expected map of QName keys")
                }
                docProp[qkey] = value
            }
            setupProperties(contentType, docProp)
        }

        for ((name, value) in overrideProperties[contentType] ?: mapOf()) {
            properties[name] = value
        }

        if (contentType.htmlContentType()) {
            if (contentType == MediaType.XHTML) {
                logger.debug { "Serializing XHTML for ${purpose}" }
                serializeXml(doc, stream)
            } else {
                logger.debug { "Serializing HTML for ${purpose}" }
                serializeHtml(doc, stream)
            }
            return
        }

        if (contentType.xmlContentType() ) {
            logger.debug { "Serializing XML for ${purpose}" }
            serializeXml(doc, stream)
            return
        }

        if (contentType.jsonContentType()) {
            logger.debug { "Serializing JSON for ${purpose}" }
            serializeJson(doc, stream)
            return
        }

        if (contentType.textContentType()) {
            logger.debug { "Serializing text for ${purpose}" }
            serializeText(doc, stream)
            return
        }

        if (doc.value is XdmNode) {
            logger.debug { "Serializing XML for ${purpose}" }
            serializeXml(doc, stream)
        } else {
            logger.debug { "Serializing JSON for ${purpose}" }
            serializeJson(doc, stream)
        }
    }

    fun write(node: XdmNode, stream: OutputStream, purpose: String, defaultProperties: Map<QName, XdmValue> = mapOf()) {
        if (S9Api.isTextDocument(node)) {
            setupProperties(MediaType.TEXT, defaultProperties)
            logger.debug { "Serializing text for ${purpose}"}
            serializeText(node, stream)
        } else {
            setupProperties(MediaType.XML, defaultProperties)
            logger.debug { "Serializing XML for ${purpose}"}
            serializeXml(node, stream)
        }
    }

    private fun setupProperties(contentType: MediaType, docprops: Map<QName, XdmValue>) {
        for ((key, value) in docprops) {
            if (value.underlyingValue is QNameValue) {
                val qname = (value.underlyingValue as QNameValue)
                properties[key] = "Q{${qname.namespaceURI}}${qname.localName}"
            } else {
                properties[key] = value.underlyingValue.stringValue
            }
        }

        for ((key, value) in overrideProperties[contentType] ?: mapOf()) {
            properties[key] = value
        }
    }

    private fun serializeXml(doc: XProcDocument, stream: OutputStream) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, "xml")
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeXml(node: XdmNode, stream: OutputStream) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer,"xml")
        serializer.serializeXdmValue(node)
    }

    private fun serializeHtml(doc: XProcDocument, stream: OutputStream) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, "html")
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeJson(doc: XProcDocument, stream: OutputStream) {
        val xs = ByteArrayOutputStream()
        val xv = processor.newSerializer(xs)
        setSerializationProperties(xv, "json")
        xv.serializeXdmValue(doc.value)
        val s = xs.toString("UTF-8")

        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, "json")
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeText(doc: XProcDocument, stream: OutputStream) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, "text")
        serializer.serializeXdmValue(doc.value)
    }

    private fun serializeText(node: XdmNode, stream: OutputStream) {
        val serializer = processor.newSerializer(stream)
        setSerializationProperties(serializer, "text")
        serializer.serializeXdmValue(node)
    }

    private fun setSerializationProperties(serializer: Serializer, method: String) {
        try {
            for ((name, value) in properties) {
                serializer.setOutputProperty(name, value)
            }

            serializer.setOutputProperty(Serializer.Property.METHOD, method)
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }
}