package com.xmlcalabash.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.QNameValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set

/**
 * Marshals documents and streams.
 */

class Marshall(val stepConfig: XProcStepConfiguration) {
    companion object {
        private val _a = QName("a")
        private val _b = QName("b")
    }

    fun convert(doc: XProcDocument, contentType: MediaType, params: Map<QName, XdmValue> = emptyMap()): XProcDocument {
        val inType = doc.contentType?.classification() ?: MediaClassification.BINARY

        when (inType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                return fromMarkup(doc, contentType)
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return fromJson(doc, contentType)
            }
            MediaClassification.TEXT -> {
                return fromText(doc, contentType, params)
            }
            MediaClassification.BINARY -> {
                return fromOther(doc as XProcBinaryDocument, contentType)
            }
        }
    }

    fun read(baseUri: URI?, stream: InputStream, contentType: MediaType, params: Map<QName, XdmValue> = emptyMap()): XProcDocument {
        val outType = contentType.classification()

        when (outType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                return toMarkup(baseUri, stream, contentType, params)
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return toJson(baseUri, stream, contentType, params)
            }
            MediaClassification.TEXT -> {
                return toText(baseUri, stream, contentType)
            }
            MediaClassification.BINARY -> {
                return toOther(baseUri, stream, contentType)
            }
        }
    }

    fun write(doc: XProcDocument, stream: OutputStream, serializationProperties: Map<QName, XdmValue> = emptyMap()) {
        val inType = doc.contentType?.classification() ?: MediaClassification.BINARY

        when (inType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                writeMarkup(doc, stream, serializationProperties)
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                writeJson(doc, stream, serializationProperties)
            }
            MediaClassification.TEXT -> {
                writeText(doc, stream, serializationProperties)
            }
            MediaClassification.BINARY -> {
                writeOther(doc as XProcBinaryDocument, stream)
            }
        }
    }

    private fun fromMarkup(doc: XProcDocument, contentType: MediaType): XProcDocument {
        val outType = contentType.classification()
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
                                    QName("", ns, stepConfig.parseNCName(key))
                                }
                                map = map.put(XdmAtomicValue(qname), XdmAtomicValue(value))
                            }
                        }
                    }
                    return doc.with(map).with(contentType, true)
                } else {
                    throw stepConfig.exception(XProcError.xiNotImplemented
                        ("${doc.contentType ?: MediaType.OCTET_STREAM} from markup"))
                }
            }
            MediaClassification.TEXT -> {
                val baos = ByteArrayOutputStream()
                val prop = mapOf<QName, XdmValue>(
                    Ns.encoding to XdmAtomicValue("UTF-8"),
                    Ns.omitXmlDeclaration to XdmAtomicValue(true)
                )
                writeMarkup(doc, baos, prop)

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
                    return XProcDocument.ofBinary(bytes, doc.context, contentType, properties)
                } else {
                    throw stepConfig.exception(
                        XProcError.xiNotImplemented
                            ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary")
                    )
                }
            }
        }
    }

    /*
    private fun serializationMethod(doc: XProcDocument): String {
        val inType = doc.contentType?.classification() ?: MediaClassification.BINARY
        return when (inType) {
            MediaClassification.XML -> "xml"
            MediaClassification.XHTML -> "xhtml"
            MediaClassification.HTML -> "html"
            MediaClassification.JSON -> "json"
            MediaClassification.YAML -> "json"
            MediaClassification.TOML -> "json"
            MediaClassification.TEXT -> "text"
            MediaClassification.BINARY -> {
                throw stepConfig.exception(XProcError.xiNotImplemented
                    ("${doc.contentType ?: MediaType.OCTET_STREAM} to text"))
            }
        }
    }

    private fun serializeDocument(doc: XProcDocument, contentType: MediaType): XProcDocument {
        val serializationProperties = mutableMapOf<QName, XdmValue>()
        doc.properties[Ns.serialization]?.let { serializationProperties.putAll(stepConfig.asMap(it as XdmMap)) }
        serializationProperties[Ns.method] = XdmAtomicValue(serializationMethod(doc))

        var map = XdmMap()
        for ((name, value) in serializationProperties) {
            if (name.namespaceUri == NamespaceUri.NULL) {
                map = map.put(XdmAtomicValue(name.localName), value)
            } else {
                map = map.put(XdmAtomicValue(name), value)
            }
        }

        val text = runFunction("serialize", listOf(doc.value, map))

        val builder = SaxonTreeBuilder(doc.context.processor)
        builder.startDocument(doc.baseURI)
        builder.addText(text.underlyingValue.stringValue)
        builder.endDocument()

        return doc.with(builder.result).with(contentType, true)
    }

     */

    private fun fromJson(doc: XProcDocument, contentType: MediaType): XProcDocument {
        val outType = contentType.classification()
        when (outType) {
            MediaClassification.XML -> {
                val baos = ByteArrayOutputStream()
                writeJson(doc, baos, emptyMap())
                val text = baos.toString(StandardCharsets.UTF_8)
                val json = runFunction("json-to-xml", listOf(XdmAtomicValue(text)))
                return doc.with(json).with(contentType, true)
            }
            MediaClassification.XHTML, MediaClassification.HTML -> {
                throw stepConfig.exception(XProcError.xiNotImplemented
                    ("${doc.contentType ?: MediaType.OCTET_STREAM} to HTML"))
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return doc.with(contentType)
            }
            MediaClassification.TEXT -> {
                val baos = ByteArrayOutputStream()
                val prop = mapOf<QName, XdmValue>(
                    Ns.encoding to XdmAtomicValue("UTF-8")
                )
                writeJson(doc, baos, prop)

                val builder = SaxonTreeBuilder(doc.context.processor)
                builder.startDocument(doc.baseURI)
                builder.addText(baos.toByteArray().toString(Charsets.UTF_8))
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }
            MediaClassification.BINARY -> {
                throw stepConfig.exception(XProcError.xiNotImplemented
                    ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary"))
            }
        }
    }

    private fun fromText(doc: XProcDocument, contentType: MediaType, params: Map<QName, XdmValue>): XProcDocument {
        val outType = contentType.classification()

        when (outType) {
            MediaClassification.XML -> {
                val text = XdmAtomicValue(doc.value.underlyingValue.stringValue)
                val xml = runFunction("parse-xml", listOf(text))
                return doc.with(xml).with(contentType, true)
            }
            MediaClassification.XHTML, MediaClassification.HTML -> {
                val text = doc.value.underlyingValue.stringValue
                val stream = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))

                val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
                val html = htmlBuilder.parse(stream)
                val builder = doc.context.processor.newDocumentBuilder()
                doc.baseURI?.let { builder.baseURI = it }
                val xdm = builder.build(DOMSource(html))
                builder.isLineNumbering = true
                return doc.with(xdm).with(contentType, true)
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                val json = runFunction("parse-json", listOf(doc.value, stepConfig.asXdmMap(params)))
                return doc.with(json).with(contentType, true)
            }
            MediaClassification.TEXT -> {
                return doc.with(contentType)
            }
            MediaClassification.BINARY -> {
                throw stepConfig.exception(XProcError.xiNotImplemented
                    ("${doc.contentType ?: MediaType.OCTET_STREAM} to binary"))
            }
        }
    }

    private fun fromOther(doc: XProcBinaryDocument, contentType: MediaType): XProcDocument {
        val outType = contentType.classification()

        when (outType) {
            MediaClassification.XML -> {
                val builder = SaxonTreeBuilder(stepConfig)
                builder.startDocument(doc.baseURI)
                val amap = mutableMapOf(
                    Ns.contentType to contentType.toString(),
                    Ns.encoding to "base64"
                )
                builder.addStartElement(NsC.data, stepConfig.attributeMap(amap))
                builder.addText(Base64.getEncoder().encodeToString(doc.binaryValue))
                builder.addEndElement()
                builder.endDocument()
                return doc.with(builder.result).with(contentType, true)
            }
             MediaClassification.BINARY -> {
                 return doc.with(contentType)
            }
            else -> {
                throw stepConfig.exception(XProcError.xiNotImplemented
                    ("${doc.contentType ?: MediaType.OCTET_STREAM} to ${contentType}"))
            }
        }
    }

    private fun toMarkup(baseUri: URI?, stream: InputStream, contentType: MediaType, params: Map<QName, XdmValue>): XProcDocument {
        val outType = contentType.classification()
        when (outType) {
            MediaClassification.XML, MediaClassification.XHTML -> {
                val saveParseOptions = stepConfig.saxonConfig.configuration.parseOptions
                val errorHandler = LoaderErrorHandler()
                val parseOptions = saveParseOptions.withErrorHandler(errorHandler)
                stepConfig.saxonConfig.configuration.parseOptions = parseOptions
                val builder = stepConfig.processor.newDocumentBuilder()
                builder.isLineNumbering = true

                /* disable loading external subset
                val cfg = context.saxonConfig.processor.underlyingConfiguration
                cfg.parseOptions = cfg.parseOptions.withParserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                 */

                val validating = if (params[Ns.dtdValidate] != null) {
                    val value = params[Ns.dtdValidate]!!.underlyingValue
                    if (value is BooleanValue) {
                        value.booleanValue
                    } else {
                        stepConfig.parseBoolean(value.stringValue)
                    }
                } else {
                    false
                }

                builder.isDTDValidation = validating
                val source = InputSource(stream)
                baseUri?.let { source.systemId = "${baseUri}" }

                try {
                    val xdm = builder.build(SAXSource(source))
                    if (errorHandler.errorCount > 0) {
                        if (validating) {
                            throw stepConfig.exception(
                                XProcError.xdNotDtdValid(
                                    errorHandler.message ?: "No message provided"
                                )
                            )
                        }
                        throw stepConfig.exception(XProcError.xdNotWellFormed())
                    }
                    return XProcDocument.ofXml(xdm, stepConfig, contentType)
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xdNotWellFormed(), ex)
                } finally {
                    stepConfig.saxonConfig.configuration.parseOptions = saveParseOptions
                }
            }
            MediaClassification.HTML -> {
                val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
                val html = htmlBuilder.parse(stream)
                val builder = stepConfig.processor.newDocumentBuilder()
                baseUri?.let { builder.baseURI = it }
                val xdm = builder.build(DOMSource(html))
                builder.isLineNumbering = true
                return XProcDocument.ofXml(xdm, stepConfig, contentType)
            }
            else -> {
                throw stepConfig.exception(XProcError.xiImpossible("Called toMarkup for ${contentType}"))
            }
        }
    }

    private fun toJson(baseUri: URI?, stream: InputStream, contentType: MediaType, params: Map<QName, XdmValue>): XProcDocument {
        val inType = contentType.classification()

        val text = toText(baseUri, stream, MediaType.TEXT).value.underlyingValue.stringValue

        when (inType) {
            MediaClassification.JSON -> {
                // Our parameters map has QName keys, but the options map has string keys
                var optmap = XdmMap()
                for ((key, value) in params) {
                    if (key.namespaceUri == NamespaceUri.NULL) {
                        optmap = optmap.put(XdmAtomicValue(key.localName), value)
                    }
                }
                try {
                    val json = runFunction("parse-json", listOf(XdmAtomicValue(text), optmap))
                    return XProcDocument.ofJson(json, stepConfig, contentType, DocumentProperties())
                } catch (ex: SaxonApiException) {
                    if ((ex.message ?: "").startsWith("Invalid option")) {
                        throw stepConfig.exception(XProcError.xdInvalidParameter(ex.message!!), ex)
                    }

                    val pos = (ex.message ?: "").indexOf("Duplicate key")
                    if (pos >= 0) {
                        val epos = ex.message!!.indexOf("}")
                        val key = ex.message!!.substring(pos+21, epos+1)
                        throw stepConfig.exception(XProcError.xdDuplicateKey(key), ex)
                    }

                    if ((ex.message ?: "").startsWith("Invalid JSON")) {
                        throw stepConfig.exception(XProcError.xdNotWellFormedJson(), ex)
                    }

                    throw ex
                }

            }
            MediaClassification.YAML -> {
                val bais = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
                val yamlReader = ObjectMapper(YAMLFactory())
                val obj = yamlReader.readValue(bais, Object::class.java)
                val jsonWriter = ObjectMapper()
                val str = jsonWriter.writeValueAsString(obj)
                val textDoc = XProcDocument.ofText(str, stepConfig, contentType, DocumentProperties())
                return fromText(textDoc, contentType, emptyMap())
            }
            MediaClassification.TOML -> {
                val bais = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
                val tomlReader = ObjectMapper(TomlFactory())
                val obj = tomlReader.readValue(bais, Object::class.java)
                val jsonWriter = ObjectMapper()
                val str = jsonWriter.writeValueAsString(obj)
                val textDoc = XProcDocument.ofText(str, stepConfig, contentType, DocumentProperties())
                return fromText(textDoc, contentType, emptyMap())
            }
            else -> {
                throw stepConfig.exception(XProcError.xiImpossible("Called toJson for ${contentType}"))
            }
        }
    }

    private fun toText(baseUri: URI?, stream: InputStream, contentType: MediaType): XProcDocument {
        val charsetName = contentType.charset() ?: "UTF-8"
        if (!Charset.isSupported(charsetName)) {
            throw stepConfig.exception(XProcError.xdUnsupportedDocumentCharset(charsetName))
        }
        val charset = Charset.forName(charsetName)
        val reader = InputStreamReader(stream, charset)
        val sb = StringBuilder()
        val buf = CharArray(4096)
        var len = reader.read(buf)
        while (len >= 0) {
            sb.appendRange(buf, 0, len)
            len = reader.read(buf)
        }
        return XProcDocument.ofText(sb.toString(), stepConfig, contentType, DocumentProperties())
    }

    private fun toOther(baseUri: URI?, stream: InputStream, contentType: MediaType): XProcDocument {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        return XProcDocument.ofBinary(baos.toByteArray(), stepConfig, contentType, DocumentProperties())
    }

    private fun writeMarkup(doc: XProcDocument, stream: OutputStream, serializationProperties: Map<QName, XdmValue> = emptyMap()) {
        val props = mutableMapOf<QName, XdmValue>()
        props.putAll(serializationProperties)
        props.putAll(stepConfig.asMap(doc.properties.getSerialization()))

        if (!props.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.XML -> props.put(Ns.method, XdmAtomicValue("xml"))
                MediaClassification.XHTML -> props.put(Ns.method, XdmAtomicValue("xhtml"))
                MediaClassification.HTML -> props.put(Ns.method, XdmAtomicValue("html"))
                else -> {
                    throw stepConfig.exception(XProcError.xiImpossible("Called writeMarkup for ${doc.contentType}"))
                }
            }
        }

        val serializer = stepConfig.processor.newSerializer(stream)
        setSerializationProperties(serializer, props)
        serializer.serializeXdmValue(doc.value)
    }

    private fun writeJson(doc: XProcDocument, stream: OutputStream, serializationProperties: Map<QName, XdmValue> = emptyMap()) {
        val props = mutableMapOf<QName, XdmValue>()
        props.putAll(serializationProperties)
        props.putAll(stepConfig.asMap(doc.properties.getSerialization()))

        if (!props.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.JSON -> props.put(Ns.method, XdmAtomicValue("json"))
                MediaClassification.YAML -> props.put(Ns.method, XdmAtomicValue(NsCx.yaml))
                MediaClassification.TOML -> props.put(Ns.method, XdmAtomicValue(NsCx.toml))
                else -> {
                    throw stepConfig.exception(XProcError.xiImpossible("Called writeJson for ${doc.contentType}"))
                }
            }
        }

        val serMethod = props[Ns.method]!!
        val method = if (serMethod.underlyingValue is QNameValue) {
            val sname = serMethod.underlyingValue as QNameValue
            QName(sname.prefix, sname.namespaceURI.toString(), sname.localName)
        } else {
            stepConfig.parseQName(serMethod.underlyingValue.stringValue)
        }

        if (method == Ns.json) {
            val serializer = stepConfig.processor.newSerializer(stream)
            setSerializationProperties(serializer, props)
            serializer.serializeXdmValue(doc.value)
            return
        }

        if (method != NsCx.yaml && method != NsCx.toml) {
            throw stepConfig.exception(XProcError.xdInvalidSerialization("${method}"))
        }

        val baos = ByteArrayOutputStream()
        val jsonSerializer = stepConfig.processor.newSerializer(baos)
        props[Ns.method] = XdmAtomicValue("json")
        props[Ns.encoding] = XdmAtomicValue("UTF-8")
        setSerializationProperties(jsonSerializer, props)
        jsonSerializer.serializeXdmValue(doc.value)

        val jsonReader = ObjectMapper(JsonFactory())
        val obj = jsonReader.readValue(baos.toString(StandardCharsets.UTF_8), Object::class.java)

        val text = if (method == NsCx.yaml) {
            val yamlWriter = ObjectMapper(YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER))
            yamlWriter.writeValueAsString(obj)
        } else { // must be TOML
            val tomlWriter = ObjectMapper(TomlFactory())
            tomlWriter.writeValueAsString(obj)
        }

        val builder = SaxonTreeBuilder(doc.context.processor)
        builder.startDocument(doc.baseURI)
        builder.addText(text)
        builder.endDocument()

        val serializer = stepConfig.processor.newSerializer(stream)
        props[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer, props)
        serializer.serializeXdmValue(builder.result)
    }

    private fun writeText(doc: XProcDocument, stream: OutputStream, serializationProperties: Map<QName, XdmValue>) {
        val props = mutableMapOf<QName, XdmValue>()
        props.putAll(serializationProperties)
        props.putAll(stepConfig.asMap(doc.properties.getSerialization()))

        if (!props.containsKey(Ns.method)) {
            props[Ns.method] = XdmAtomicValue("text")
        }

        val serializer = stepConfig.processor.newSerializer(stream)
        props[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer, props)
        serializer.serializeXdmValue(doc.value)
    }

    private fun writeOther(doc: XProcBinaryDocument, stream: OutputStream) {
        stream.write(doc.binaryValue)
    }

    private fun setSerializationProperties(serializer: Serializer, properties: Map<QName, XdmValue>) {
        try {
            for ((name, value) in properties) {
                if (value.underlyingValue is QNameValue) {
                    val qname = (value.underlyingValue as QNameValue)
                    serializer.setOutputProperty(name, "Q{${qname.namespaceURI}}${qname.localName}")
                } else {
                    serializer.setOutputProperty(name, value.underlyingValue.stringValue)
                }
            }
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }

    private fun runFunction(function: String, values: List<XdmValue>): XdmValue {
        var compiler = stepConfig.newXPathCompiler()

        when (values.size) {
            0 -> {
                val selector = compiler.compile("${function}()").load()
                selector.resourceResolver = stepConfig.environment.documentManager
                return selector.evaluate()
            }
            1 -> {
                compiler.declareVariable(_a)
                val selector = compiler.compile("${function}(\$a)").load()
                selector.setVariable(QName("a"), values[0])
                selector.resourceResolver = stepConfig.environment.documentManager
                return selector.evaluate()
            }
            2 -> {
                compiler.declareVariable(_a)
                compiler.declareVariable(_b)
                val selector = compiler.compile("${function}(\$a, \$b)").load()
                selector.setVariable(QName("a"), values[0])
                selector.setVariable(QName("b"), values[1])
                selector.resourceResolver = stepConfig.environment.documentManager
                return selector.evaluate()
            }
            else -> {
                throw stepConfig.exception(XProcError.xiImpossible("Called runFunction with more than two arguments"))
            }
        }
    }

    private class LoaderErrorHandler(): ErrorHandler {
        var errorCount = 0
        var message: String? = null

        override fun warning(exception: SAXParseException?) {
            // nop
        }

        override fun error(exception: SAXParseException?) {
            if (message == null && exception?.message != null) {
                message = exception.message
            }
            errorCount++
        }

        override fun fatalError(exception: SAXParseException?) {
            if (message == null && exception?.message != null) {
                message = exception.message
            }
            errorCount++
        }

    }

}