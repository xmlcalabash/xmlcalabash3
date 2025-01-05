package com.xmlcalabash.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.BooleanValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.*
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.util.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource

class DocumentLoader(val context: XProcStepConfiguration,
                     val href: URI?,
                     val documentProperties: DocumentProperties = DocumentProperties(),
                     val parameters: Map<QName,XdmValue> = mapOf()) {
    companion object {
        val cx_can_read = QName(NsCx.namespace, "cx:can-read")
        val cx_can_write = QName(NsCx.namespace, "cx:can-write")
        val cx_can_execute = QName(NsCx.namespace, "cx:can-execute")
        val cx_is_hidden = QName(NsCx.namespace, "cx:is-hidden")
        val cx_last_modified = QName(NsCx.namespace, "cx:last-modified")
        val cx_size = QName(NsCx.namespace, "cx:size")
    }

    var mediaType = MediaType.XML
    val properties = DocumentProperties()
    var absURI: URI = UriUtils.cwdAsUri()

    fun load(): XProcDocument {
        if (href == null) {
            throw context.exception(XProcError.xiImpossible("Attempt to load document with no URI"))
        }

        absURI = if (href.isAbsolute) {
            href
        } else {
            if (context.baseUri != null) {
                context.baseUri!!.resolve(href)
            } else {
                UriUtils.cwdAsUri().resolve(href)
            }
        }

        if (absURI.scheme == "file") {
            try {
                return loadFile()
            } catch (ex: IOException) {
                throw context.exception(XProcError.xdDoesNotExist(absURI.path), ex)
            }
        }

        if (absURI.scheme == "http" || absURI.scheme == "https") {
            val req = InternetProtocolRequest(context, absURI)
            val resp = req.execute("GET")

            if (resp.statusCode >= 500) {
                throw context.exception(XProcError.xdIsNotReadable(absURI.toString(), "HTTP response code: ${resp.statusCode}"))
            }
            if (resp.statusCode >= 400) {
                throw context.exception(XProcError.xdDoesNotExist(absURI.toString()))
            }

            return resp.response.first()
        }

        throw RuntimeException("Unsupported scheme")
    }

    private fun loadFile(): XProcDocument {
        val file = File(absURI.path)

        mediaType = if (documentProperties.has(Ns.contentType)) {
            MediaType.parse(documentProperties[Ns.contentType]!!.underlyingValue.stringValue)
        } else {
            val fileMediaType = context.environment.mimeTypes.getContentType(absURI.toString())
            MediaType.parse(fileMediaType)
        }

        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType

        if (properties.baseURI == null) {
            properties[Ns.baseUri] = file.toURI()
        }

        properties[cx_can_read] = file.canRead()
        properties[cx_can_write] = file.canWrite()
        properties[cx_can_execute] = file.canExecute()
        properties[cx_is_hidden] = file.isHidden
        properties[cx_size] = XdmAtomicValue(file.length())

        val lmDate = Date(file.lastModified())
        properties[cx_last_modified] = XdmAtomicValue(lmDate.toInstant().atZone(ZoneOffset.UTC))

        val stream = FileInputStream(file)
        return load(absURI, stream, mediaType)
    }

    fun load(uri: URI?, stream: InputStream, mediaType: MediaType): XProcDocument {
        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType
        if (uri != null && properties.baseURI == null) {
            properties[Ns.baseUri] = uri
        }

        val classification = mediaType.classification()
        when (classification) {
            MediaClassification.XML, MediaClassification.XHTML -> {
                try {
                    return loadXml(uri, stream)
                } catch (ex: SaxonApiException) {
                    throw context.exception(XProcError.xdNotWellFormed(), ex)
                }
            }
            MediaClassification.HTML -> return loadHtml(uri, stream)
            MediaClassification.JSON -> return loadJson(stream)
            MediaClassification.TEXT -> return loadText(stream)
            // I'm not sure what to do with CSV. Maybe wait for XPath 4?
            // MediaType.CSV -> return loadCsv(stream)
            MediaClassification.YAML -> return loadYaml(stream)
            MediaClassification.TOML -> return loadToml(stream)
            else -> return loadBinary(stream)
        }
    }

    private fun loadXml(uri: URI?, stream: InputStream): XProcDocument {
        val saveParseOptions = context.saxonConfig.configuration.parseOptions
        val errorHandler = LoaderErrorHandler()
        val parseOptions = saveParseOptions.withErrorHandler(errorHandler)
        context.saxonConfig.configuration.parseOptions = parseOptions
        val builder = context.processor.newDocumentBuilder()
        builder.isLineNumbering = true

        /* disable loading external subset
        val cfg = context.saxonConfig.processor.underlyingConfiguration
        cfg.parseOptions = cfg.parseOptions.withParserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
         */

        val validating = if (parameters[Ns.dtdValidate] != null) {
            val value = parameters[Ns.dtdValidate]!!.underlyingValue
            if (value is BooleanValue) {
                value.booleanValue
            } else {
                context.parseBoolean(value.stringValue)
            }
        } else {
            false
        }

        builder.isDTDValidation = validating
        val source = InputSource(stream)
        if (uri != null) {
            source.systemId = uri.toString();
        }

        try {
            val xdm = builder.build(SAXSource(source))
            if (errorHandler.errorCount > 0) {
                if (validating) {
                    throw context.exception(XProcError.xdNotDtdValid(errorHandler.message ?: "No message provided"))
                }
                throw context.exception(XProcError.xdNotWellFormed())
            }
            return XProcDocument.ofXml(xdm, context, properties)
        } finally {
            context.saxonConfig.configuration.parseOptions = saveParseOptions
        }
    }

    private fun loadHtml(uri: URI?, stream: InputStream): XProcDocument {
        val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
        val html = htmlBuilder.parse(stream)
        val builder = context.processor.newDocumentBuilder()
        if (uri != null) {
            builder.baseURI = uri
        }
        val xdm = builder.build(DOMSource(html))
        builder.isLineNumbering = true
        return XProcDocument.ofXml(xdm, context, properties)
    }

    private fun loadJson(stream: InputStream): XProcDocument {
        val compiler = context.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        compiler.declareVariable(QName("opt"))
        val selector = compiler.compile("parse-json(\$a, \$opt)").load()
        selector.resourceResolver = context.environment.documentManager
        val inputjson = loadTextData(stream)
        selector.setVariable(QName("a"), XdmAtomicValue(inputjson))

        // Our parameters map has QName keys, but the options map has string keys
        var optmap = XdmMap()
        for ((key, value) in parameters) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                optmap = optmap.put(XdmAtomicValue(key.localName), value)
            }
        }
        selector.setVariable(QName("opt"), optmap)

        try {
            val json = selector.evaluate()
            return XProcDocument(json, context, properties)
        } catch (ex: SaxonApiException) {
            if ((ex.message ?: "").startsWith("Invalid option")) {
                throw context.exception(XProcError.xdInvalidParameter(ex.message!!), ex)
            }

            val pos = (ex.message ?: "").indexOf("Duplicate key")
            if (pos >= 0) {
                val epos = ex.message!!.indexOf("}")
                val key = ex.message!!.substring(pos+21, epos+1)
                throw context.exception(XProcError.xdDuplicateKey(key), ex)
            }

            if ((ex.message ?: "").startsWith("Invalid JSON")) {
                throw context.exception(XProcError.xdNotWellFormedJson(), ex)
            }

            throw ex
        }
    }

    private fun loadYaml(stream: InputStream): XProcDocument {
        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(stream, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        // Hack
        return loadJson(ByteArrayInputStream(str.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun loadToml(stream: InputStream): XProcDocument {
        val tomlReader = ObjectMapper(TomlFactory())
        val obj = tomlReader.readValue(stream, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        // Hack
        return loadJson(ByteArrayInputStream(str.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun loadCsv(stream: InputStream): XProcDocument {
        throw RuntimeException("Bang")
    }

    private fun loadText(stream: InputStream): XProcDocument {
        val builder = SaxonTreeBuilder(context.processor)
        builder.startDocument(context.baseUri)
        builder.addText(loadTextData(stream))
        builder.endDocument()
        return XProcDocument.ofXml(builder.result, context, properties)
    }

    private fun loadBinary(stream: InputStream): XProcDocument {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        return XProcDocument.ofBinary(baos.toByteArray(), context, properties)
    }

    private fun loadTextData(stream: InputStream): String {
        val charsetName = mediaType.charset() ?: "UTF-8"
        if (!Charset.isSupported(charsetName)) {
            throw context.exception(XProcError.xdUnsupportedDocumentCharset(charsetName))
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
        return sb.toString()
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