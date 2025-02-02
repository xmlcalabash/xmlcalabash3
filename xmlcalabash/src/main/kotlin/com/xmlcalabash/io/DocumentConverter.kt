package com.xmlcalabash.io

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.transform.dom.DOMSource

class DocumentConverter(val stepConfig: XProcStepConfiguration,
                        val doc: XProcDocument,
                        val contentType: MediaType,
                        externalSerialization: Map<QName, XdmValue> = emptyMap()): Marshaller(stepConfig) {
    private val outType = contentType.classification()
    private val _params = mutableMapOf<QName, XdmValue>()
    lateinit private var inType: MediaClassification
    val serializationParameters: Map<QName, XdmValue>
        get() = _params

    init {
        _params.putAll(externalSerialization)
        _params.putAll(stepConfig.asMap(doc.properties.getSerialization()))
    }

    operator fun get(name: QName): XdmValue? {
        return _params[name]
    }

    operator fun set(name: QName, value: XdmValue) {
        _params[name] = value
    }

    operator fun set(name: QName, value: String) {
        _params[name] = XdmAtomicValue(value)
    }

    operator fun set(name: QName, value: Boolean) {
        _params[name] = XdmAtomicValue(value)
    }

    fun convert(): XProcDocument {
        inType = doc.contentType?.classification() ?: MediaClassification.BINARY
        when (inType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                return fromMarkup()
            }

            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return fromJson()
            }

            MediaClassification.TEXT -> {
                return fromText()
            }

            MediaClassification.BINARY -> {
                return fromOther()
            }
        }
    }

    private fun fromMarkup(): XProcDocument {
        val element = S9Api.documentElement(doc.value as XdmNode)
        if (element.nodeName == NsC.data) {
            return decoded(element)
        }

        when (outType) {
            MediaClassification.XML -> {
                return doc.with(contentType)
            }

            MediaClassification.XHTML, MediaClassification.HTML -> {
                return doc.with(contentType)
            }

            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                val root = S9Api.documentElement(doc.value as XdmNode)
                if (root.nodeName.namespaceUri == NsFn.namespace) {
                    val jsonString = runFunction("xml-to-json", listOf(doc.value))
                    val result = runFunction("parse-json", listOf(jsonString))
                    return doc.with(result).with(contentType, true)
                } else if (root.nodeName == NsC.paramSet) {
                    var map = XdmMap()
                    for (child in root.axisIterator(Axis.CHILD)) {
                        if (child.nodeKind == XdmNodeKind.ELEMENT && child.nodeName == NsC.param) {
                            val key = child.getAttributeValue(Ns.name)
                            val ns = child.getAttributeValue(Ns.namespace)
                            val value = child.getAttributeValue(Ns.value)
                            if (key != null && value != null) {
                                val qname = if (ns == null) {
                                    QName(key, child)
                                } else {
                                    QName("", ns, docContext.parseNCName(key))
                                }
                                map = map.put(XdmAtomicValue(qname), XdmAtomicValue(value))
                            }
                        }
                    }
                    return doc.with(map).with(contentType, true)
                } else {
                    throw stepConfig.exception(
                        XProcError.xiCastUnsupported
                            ("${doc.contentType ?: MediaType.OCTET_STREAM} from markup")
                    )
                }
            }

            MediaClassification.TEXT -> {
                if (contentType == MediaType.JAVA_PROPERTIES) {
                    if (inType == MediaClassification.XML ) {
                        return javaPropertiesFromMarkup()
                    } else {
                        throw stepConfig.exception(
                            XProcError.xiCastUnsupported
                                ("${doc.contentType ?: MediaType.OCTET_STREAM} from ${doc.contentType}")
                        )
                    }
                }
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos, serializationParameters)
                writer[Ns.encoding] = XdmAtomicValue("UTF-8")
                writer[Ns.omitXmlDeclaration] = XdmAtomicValue(true)
                writer.write()

                val builder = SaxonTreeBuilder(doc.context.processor)
                builder.startDocument(doc.baseURI)
                builder.addText(baos.toByteArray().toString(Charsets.UTF_8))
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }

            MediaClassification.BINARY -> {
                val node = S9Api.documentElement(doc.value as XdmNode)
                if (node.nodeName == NsC.data) {
                    return decoded(node)
                } else {
                    throw stepConfig.exception(
                        XProcError.xiNotImplemented
                            ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                    )
                }
            }
        }
    }

    private fun javaPropertiesFromMarkup(): XProcDocument {
        val root = S9Api.documentElement(doc.value as XdmNode)
        if (root.nodeName != QName(NamespaceUri.NULL, "properties")) {
            throw stepConfig.exception(XProcError.xiCastInputIncorrect("Unsupported root element: ${root.nodeName}"))
        }
        var comment: String? = null
        val properties = Properties()
        for (child in root.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.ELEMENT -> {
                    if (child.nodeName == QName(NamespaceUri.NULL, "comment")) {
                        comment = child.underlyingNode.stringValue
                    } else if (child.nodeName == QName(NamespaceUri.NULL, "entry")) {
                        val key = child.getAttributeValue(Ns.key)
                        if (key == null) {
                            throw stepConfig.exception(XProcError.xiCastInputIncorrect("No key attribute on entry"))
                        }
                        properties.setProperty(key, child.underlyingNode.stringValue)
                    } else {
                        throw stepConfig.exception(XProcError.xiCastInputIncorrect("Unsupported element: ${child.nodeName}"))
                    }
                }
                XdmNodeKind.TEXT -> {
                    if (child.underlyingNode.stringValue.trim().isNotBlank()) {
                        throw stepConfig.exception(XProcError.xiCastInputIncorrect("Unsupported non-whitespace text: ${child.underlyingNode.stringValue.trim()}"))
                    }
                }
                else -> Unit
            }
        }

        // It irks me that storing properties always adds a date comment...
        val sb = StringBuilder()
        if (comment != null) {
            for (line in comment.split("\n")) {
                sb.append("#").append(line).append("\n")
            }
        }

        val baos = ByteArrayOutputStream()
        properties.store(baos, null)
        for (line in baos.toString(Charsets.UTF_8).split("\n")) {
            if (!line.startsWith("#")) {
                sb.append(line).append("\n")
            }
        }

        val text = sb.toString()
        return XProcDocument.ofText(text, stepConfig).with(contentType, true)
    }

    private fun fromJson(): XProcDocument {
        when (outType) {
            MediaClassification.XML -> {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos, serializationParameters)
                writer[Ns.encoding] = XdmAtomicValue("UTF-8")
                writer[Ns.method] = "json"
                writer.write()

                val text = baos.toString(StandardCharsets.UTF_8)
                val json = runFunction("json-to-xml", listOf(XdmAtomicValue(text)))
                return doc.with(json).with(contentType, true)
            }

            MediaClassification.XHTML, MediaClassification.HTML -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to HTML")
                )
            }

            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return doc.with(contentType)
            }

            MediaClassification.TEXT -> {
                if (contentType == MediaType.JAVA_PROPERTIES) {
                    return toJavaProperties()
                }
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos, serializationParameters)
                writer[Ns.encoding] = XdmAtomicValue("UTF-8")
                writer.write()

                val builder = SaxonTreeBuilder(doc.context.processor)
                builder.startDocument(doc.baseURI)
                builder.addText(baos.toByteArray().toString(Charsets.UTF_8))
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }

            MediaClassification.BINARY -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                )
            }
        }
    }

    private fun toJavaProperties(): XProcDocument {
        if (doc.value !is XdmMap) {
            throw stepConfig.exception(XProcError.xiCastUnsupported("Only JSON maps can be cast to ${contentType}"))
        }

        val properties = Properties()
        val map = doc.value as XdmMap
        for (mapKey in map.keySet()) {
            val key = mapKey.underlyingValue.stringValue
            val mapValue = map.get(mapKey)
            if (mapValue is XdmFunctionItem) {
                throw stepConfig.exception(XProcError.xiCastUnsupported("Array/map/function values cannot be cast to ${contentType}"))
            }
            properties.setProperty(key, mapValue.underlyingValue.stringValue)
        }

        // It irks me that storing properties always adds a date comment...
        val sb = StringBuilder()
        val baos = ByteArrayOutputStream()
        properties.store(baos, null)
        for (line in baos.toString(Charsets.UTF_8).split("\n")) {
            if (!line.startsWith("#")) {
                sb.append(line).append("\n")
            }
        }

        val text = sb.toString()
        return XProcDocument.ofText(text, stepConfig).with(contentType, true)
    }

    private fun fromText(): XProcDocument {
        if (doc.contentType == MediaType.JAVA_PROPERTIES) {
            return fromJavaProperties()
        }
        return fromPlainText()
    }

    private fun fromPlainText(): XProcDocument {
        when (outType) {
            MediaClassification.XML -> {
                try {
                    val text = XdmAtomicValue(doc.value.underlyingValue.stringValue)
                    val xml = runFunction("parse-xml", listOf(text))
                    return doc.with(xml).with(contentType, true)
                } catch (ex: SaxonApiException) {
                    throw stepConfig.exception(XProcError.xdNotWellFormed())
                }
            }

            MediaClassification.XHTML, MediaClassification.HTML -> {
                val text = doc.value.underlyingValue.stringValue
                val stream = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))

                val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
                val html = htmlBuilder.parse(stream)
                val builder = doc.context.processor.newDocumentBuilder()

                if (doc.baseURI != null && doc.baseURI.toString().isNotEmpty()) {
                    builder.baseURI = doc.baseURI!!
                }

                val xdm = builder.build(DOMSource(html))
                builder.isLineNumbering = true
                return doc.with(xdm).with(contentType, true)
            }

            MediaClassification.JSON -> {
                try {
                    val params = stepConfig.asXdmMap(serializationParameters)
                    val json = runFunction("parse-json", listOf(doc.value, params))
                    return doc.with(json).with(contentType, true)
                } catch (ex: SaxonApiException) {
                    throw stepConfig.exception(XProcError.xdNotWellFormedJson())
                }
            }

            MediaClassification.YAML, MediaClassification.TOML -> {
                val text = doc.value.underlyingValue.stringValue
                val bais = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
                return DocumentReader(stepConfig, bais, contentType).read()
            }

            MediaClassification.TEXT -> {
                return doc.with(contentType)
            }

            MediaClassification.BINARY -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                )
            }
        }
    }

    private fun fromJavaProperties(): XProcDocument {
        // Java makes no effort to preserve the comment, but since it can be in the XML...
        val text = doc.value.underlyingValue.stringValue.trim()
        val commentBuilder = StringBuilder()
        for (line in text.split('\n')) {
            if (line.trim().startsWith('#')) {
                commentBuilder.append(line.trim().substring(1)).append("\n")
            } else {
                break
            }
        }
        val comment = commentBuilder.toString().trimEnd()

        // Is this a properties file?
        val properties = Properties()
        val istream = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
        properties.load(istream)

        when (outType) {
            MediaClassification.XML -> {
                val _properties = QName("properties")
                val _entry = QName("entry")

                val builder = SaxonTreeBuilder(stepConfig)
                builder.startDocument(doc.baseURI)
                builder.addStartElement(_properties, stepConfig.attributeMap(mapOf(
                    Ns.version to "1.0"
                )))
                if (comment.isNotBlank()) {
                    builder.addStartElement(Ns.comment)
                    builder.addText(comment)
                    builder.addEndElement()
                }
                for (key in properties.stringPropertyNames()) {
                    builder.addStartElement(_entry, stepConfig.attributeMap(mapOf(
                        Ns.key to key
                    )))
                    builder.addText(properties.getProperty(key))
                    builder.addEndElement()
                }
                builder.addEndElement()
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }

            MediaClassification.XHTML, MediaClassification.HTML -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to HTML or XHTML")
                )
            }

            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                var xdmMap = XdmMap()
                for (key in properties.stringPropertyNames()) {
                    xdmMap = xdmMap.put(XdmAtomicValue(key), XdmAtomicValue(properties.getProperty(key)))
                }
                return doc.with(xdmMap).with(contentType, true)
            }

            MediaClassification.TEXT -> {
                return doc.with(contentType)
            }

            MediaClassification.BINARY -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                )
            }
        }
    }

    private fun fromOther(): XProcDocument {
        when (outType) {
            MediaClassification.XML -> {
                val builder = SaxonTreeBuilder(stepConfig)
                builder.startDocument(doc.baseURI)
                val amap = mutableMapOf(
                    Ns.contentType to "${doc.contentType ?: MediaType.OCTET_STREAM}",
                    Ns.encoding to "base64"
                )
                builder.addStartElement(NsC.data, stepConfig.attributeMap(amap))
                builder.addText(Base64.getEncoder().encodeToString((doc as XProcBinaryDocument).binaryValue))
                builder.addEndElement()
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }

            MediaClassification.BINARY -> {
                return doc.with(contentType)
            }

            else -> {
                throw stepConfig.exception(
                    XProcError.xiCastUnsupported
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to ${contentType}")
                )
            }
        }
    }

    private fun decoded(node: XdmNode): XProcDocument {
        val dataContentTypeAttr = node.getAttributeValue(Ns.contentType)
            ?: throw stepConfig.exception(XProcError.xcContentTypeRequired())

        val dataContentType = MediaType.parse(dataContentTypeAttr)
        if (contentType != dataContentType) {
            throw stepConfig.exception(XProcError.xcDifferentContentTypes(contentType, dataContentType))
        }

        val encoding = node.getAttributeValue(Ns.encoding) ?: "base64"
        if (encoding != "base64") {
            throw stepConfig.exception(XProcError.xcInvalidEncoding(encoding))
        }

        val bytes = try {
            Base64.getDecoder().decode(node.stringValue)
        } catch (ex: IllegalArgumentException) {
            throw stepConfig.exception(XProcError.xcNotBase64(ex.message ?: "Base64 decoding error"))
        }

        val properties = DocumentProperties(doc.properties)
        properties.remove(Ns.serialization)

        if (contentType.classification() == MediaClassification.BINARY) {
            return XProcDocument.ofBinary(bytes, doc.context, contentType, properties).with(contentType, true)
        }

        val charset = node.getAttributeValue(Ns.charset) ?: "utf-8"
        val string = try {
            bytes.toString(Charset.forName(charset))
        } catch (ex: IllegalArgumentException) {
            throw stepConfig.exception(XProcError.xcInvalidCharset(charset), ex)
        }

        val loader = DocumentLoader(stepConfig, null, properties)
        val bais = ByteArrayInputStream(string.toByteArray())
        return loader.load(null, bais, contentType)
    }
}