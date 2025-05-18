package com.xmlcalabash.io

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.TypeUtils
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.QNameValue
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.collections.iterator

class DocumentWriter(val doc: XProcDocument,
                     val stream: OutputStream,
                     externalSerialization: Map<QName, XdmValue> = emptyMap()): Marshaller(doc.context) {
    private val _params = mutableMapOf<QName, XdmValue>()
    val inType = doc.contentType?.classification() ?: MediaClassification.BINARY
    val serializationParameters: Map<QName, XdmValue>
        get() = _params
    init {
        val inputMap = doc.properties.getSerialization()
        for (key in inputMap.keySet()) {
            val value = inputMap.get(key)
            val qvalue = key.underlyingValue
            val qkey = if (qvalue is QNameValue) {
                QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
            } else {
                throw RuntimeException("Expected map of QName keys")
            }
            _params[qkey] = value
        }
        _params.putAll(externalSerialization)
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

    fun write() {
        when (inType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                writeMarkup()
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                writeJson()
            }
            MediaClassification.TEXT -> {
                writeText()
            }
            MediaClassification.BINARY -> {
                writeOther()
            }
        }
    }

    private fun writeMarkup() {
        if (!_params.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.XML -> _params.put(Ns.method, XdmAtomicValue("xml"))
                MediaClassification.XHTML -> _params.put(Ns.method, XdmAtomicValue("xhtml"))
                MediaClassification.HTML -> _params.put(Ns.method, XdmAtomicValue("html"))
                else -> {
                    throw XProcError.xiImpossible("Called writeMarkup for ${doc.contentType}").exception()
                }
            }
        }

        val serializer = docContext.processor.newSerializer(stream)
        setSerializationProperties(serializer)
        serializer.serializeXdmValue(doc.value)
    }

    private fun writeJson() {
        if (!_params.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.JSON -> _params.put(Ns.method, XdmAtomicValue("json"))
                MediaClassification.YAML -> _params.put(Ns.method, XdmAtomicValue(NsCx.yaml))
                MediaClassification.TOML -> _params.put(Ns.method, XdmAtomicValue(NsCx.toml))
                else -> _params.put(Ns.method, XdmAtomicValue("json"))
            }
        }

        val serMethod = _params[Ns.method]!!
        val method = if (serMethod.underlyingValue is QNameValue) {
            val sname = serMethod.underlyingValue as QNameValue
            QName(sname.prefix, sname.namespaceURI.toString(), sname.localName)
        } else {
            val typeUtils = TypeUtils(docContext)
            typeUtils.parseQName(serMethod.underlyingValue.stringValue)
        }

        if (method == Ns.json || method == Ns.adaptive) {
            val serializer = docContext.processor.newSerializer(stream)
            setSerializationProperties(serializer)
            serializer.serializeXdmValue(doc.value)
            return
        }

        if (method != NsCx.yaml && method != NsCx.toml) {
            throw XProcError.xdInvalidSerialization("${method}").exception()
        }

        val saveParams = mutableMapOf<QName, XdmValue>()
        saveParams.putAll(_params)
        _params.clear()

        val baos = ByteArrayOutputStream()
        val jsonSerializer = docContext.processor.newSerializer(baos)
        _params[Ns.method] = XdmAtomicValue("json")
        _params[Ns.encoding] = XdmAtomicValue("UTF-8")
        setSerializationProperties(jsonSerializer)
        jsonSerializer.serializeXdmValue(doc.value)

        _params.clear()
        _params.putAll(saveParams)

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

        val serializer = docContext.processor.newSerializer(stream)
        _params[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer)
        serializer.serializeXdmValue(builder.result)
    }

    private fun writeText() {
        if (!_params.containsKey(Ns.method)) {
            _params[Ns.method] = XdmAtomicValue("text")
        }

        val serializer = docContext.processor.newSerializer(stream)
        _params[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer)
        serializer.serializeXdmValue(doc.value)
    }

    private fun writeOther() {
        if (doc is XProcBinaryDocument) {
            stream.write(doc.binaryValue)
        } else {
            stream.write(doc.value.toString().toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun setSerializationProperties(serializer: Serializer) {
        try {
            for ((name, value) in _params) {
                if (value.underlyingValue is QNameValue) {
                    val qname = (value.underlyingValue as QNameValue)
                    serializer.setOutputProperty(name, "Q{${qname.namespaceURI}}${qname.localName}")
                } else {
                    // Ignore the empty sequence...
                    if (value != XdmEmptySequence.getInstance()) {
                        serializer.setOutputProperty(name, value.underlyingValue.stringValue)
                    }
                }
            }
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }
}