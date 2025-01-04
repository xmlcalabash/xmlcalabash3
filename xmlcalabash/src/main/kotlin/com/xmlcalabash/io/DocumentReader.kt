package com.xmlcalabash.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import kotlin.collections.iterator

class DocumentReader(val stepConfig: XProcStepConfiguration,
                     val stream: InputStream,
                     val contentType: MediaType,
                     val baseUri: URI?= null,
                     param: Map<QName, XdmValue> = emptyMap()): Marshaller(stepConfig) {
    val outType = contentType.classification()
    private val _params = mutableMapOf<QName, XdmValue>()
    val params: Map<QName, XdmValue>
        get() = _params
    init {
        _params.putAll(param)
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

    fun read(): XProcDocument {
        when (outType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                return readMarkup()
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                return readJson()
            }
            MediaClassification.TEXT -> {
                return readText()
            }
            MediaClassification.BINARY -> {
                return readOther()
            }
        }
    }

    private fun readMarkup(): XProcDocument {
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

    private fun readJson(): XProcDocument {
        val inType = contentType.classification()

        // Might be JSON, but might also be YAML or TOML
        val reader = DocumentReader(stepConfig, stream, MediaType.TEXT, baseUri)
        val text = reader.read().value.underlyingValue.stringValue

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
                val textDoc = XProcDocument.ofText(str, stepConfig, MediaType.TEXT, DocumentProperties())

                val converter = DocumentConverter(stepConfig, textDoc, MediaType.JSON, emptyMap())
                return converter.convert().with(contentType)
            }
            MediaClassification.TOML -> {
                val bais = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
                val tomlReader = ObjectMapper(TomlFactory())
                val obj = tomlReader.readValue(bais, Object::class.java)
                val jsonWriter = ObjectMapper()
                val str = jsonWriter.writeValueAsString(obj)
                val textDoc = XProcDocument.ofText(str, stepConfig, MediaType.TEXT, DocumentProperties())

                val converter = DocumentConverter(stepConfig, textDoc, MediaType.JSON, emptyMap())
                return converter.convert().with(contentType)
            }
            else -> {
                throw stepConfig.exception(XProcError.xiImpossible("Called toJson for ${contentType}"))
            }
        }
    }

    private fun readText(): XProcDocument {
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

        val props = DocumentProperties()
        baseUri?.let { props[Ns.baseUri] = it }
        return XProcDocument.ofText(sb.toString(), stepConfig, contentType, props)
    }

    private fun readOther(): XProcDocument {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        val props = DocumentProperties()
        baseUri?.let { props[Ns.baseUri] = it }
        return XProcDocument.ofBinary(baos.toByteArray(), stepConfig, contentType, props)
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