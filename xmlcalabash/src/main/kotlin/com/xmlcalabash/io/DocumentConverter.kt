package com.xmlcalabash.io

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
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.xml.transform.dom.DOMSource
import kotlin.collections.iterator

class DocumentConverter(val stepConfig: XProcStepConfiguration,
                        val doc: XProcDocument,
                        val contentType: MediaType,
                        externalSerialization: Map<QName, XdmValue> = emptyMap()): Marshaller(stepConfig) {
    val outType = contentType.classification()
    private val _params = mutableMapOf<QName, XdmValue>()
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
        val inType = doc.contentType?.classification() ?: MediaClassification.BINARY
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
        when (outType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
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
                        XProcError.xiNotImplemented
                            ("${doc.contentType ?: MediaType.OCTET_STREAM} from markup")
                    )
                }
            }

            MediaClassification.TEXT -> {
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
                    val dataContentTypeAttr = node.getAttributeValue(Ns.contentType)
                        ?: throw stepConfig.exception(XProcError.xcContentTypeRequired())
                    val dataContentType = MediaType.parse(dataContentTypeAttr)
                    if (contentType != dataContentType) {
                        throw stepConfig.exception(XProcError.xcDifferentContentTypes(contentType, dataContentType))
                    }

                    val bytes = try {
                        Base64.getDecoder().decode(node.stringValue)
                    } catch (ex: IllegalArgumentException) {
                        throw stepConfig.exception(XProcError.xcNotBase64(ex.message ?: "Base64 decoding error"))
                    }

                    val properties = doc.properties
                    properties.remove(Ns.serialization)
                    return XProcDocument.ofBinary(bytes, doc.context, contentType, properties).with(contentType, true)
                } else {
                    throw stepConfig.exception(
                        XProcError.xiNotImplemented
                            ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                    )
                }
            }
        }
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
                    XProcError.xiNotImplemented
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to HTML")
                )
            }

            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return doc.with(contentType)
            }

            MediaClassification.TEXT -> {
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
                    XProcError.xiNotImplemented
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                )
            }
        }
    }

    private fun fromText(): XProcDocument {
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
                    XProcError.xiNotImplemented
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
                    XProcError.xiNotImplemented
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} to ${contentType}")
                )
            }
        }
    }
}