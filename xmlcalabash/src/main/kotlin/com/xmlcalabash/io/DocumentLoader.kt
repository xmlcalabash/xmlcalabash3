package com.xmlcalabash.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.spi.ContentTypeLoader
import com.xmlcalabash.spi.ContentTypeLoaderServiceProvider
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.yaml.snakeyaml.error.MarkedYAMLException
import java.io.*
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.util.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource

class DocumentLoader(val stepConfig: StepConfiguration,
                     val href: URI?,
                     val documentProperties: DocumentProperties = DocumentProperties(),
                     val parameters: Map<QName,XdmValue> = mapOf(),
                     val originalURI: URI? = null) {
    companion object {
        val cx_can_read = QName(NsCx.namespace, "cx:can-read")
        val cx_can_write = QName(NsCx.namespace, "cx:can-write")
        val cx_can_execute = QName(NsCx.namespace, "cx:can-execute")
        val cx_is_hidden = QName(NsCx.namespace, "cx:is-hidden")
        val cx_last_modified = QName(NsCx.namespace, "cx:last-modified")
        val cx_size = QName(NsCx.namespace, "cx:size")
        private var contentTypeLoaders: List<ContentTypeLoader>? = null

        private val FF = (-1).toByte()
        private val FE = (-2).toByte()

        fun readTextStream(stream: InputStream, suppliedCharset: Charset?): String {
            val bytes = stream.readAllBytes()

            val charset = suppliedCharset
                ?: if (bytes.size < 2) {
                    StandardCharsets.UTF_8
                } else {
                    if (bytes[0] == FF && bytes[1] == FE) {
                        StandardCharsets.UTF_16LE
                    } else if (bytes[0] == FE && bytes[1] == FF) {
                        StandardCharsets.UTF_16BE
                    } else {
                        StandardCharsets.UTF_8
                    }
                }

            val chars = charset.decode(ByteBuffer.wrap(bytes))

            val sb = StringBuilder()
            when (charset) {
                StandardCharsets.UTF_8, StandardCharsets.UTF_16,
                StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE, -> {
                    if (chars.length > 0 && chars[0] == '\uFEFF') {
                        sb.append(chars, 1, chars.remaining())
                    } else {
                        sb.append(chars)
                    }
                }
                else -> {
                    sb.append(chars)
                }
            }

            return sb.toString()
        }
    }

    var mediaType = MediaType.XML
    val properties = DocumentProperties()
    var absURI: URI = UriUtils.cwdAsUri()

    fun load(): XProcDocument {
        if (href == null) {
            throw stepConfig.exception(XProcError.xiImpossible("Attempt to load document with no URI"))
        }

        absURI = if (href.isAbsolute) {
            href
        } else {
            if (stepConfig.baseUri != null) {
                stepConfig.baseUri!!.resolve(href)
            } else {
                UriUtils.cwdAsUri().resolve(href)
            }
        }

        if (absURI.scheme == "file") {
            try {
                return loadFile()
            } catch (ex: Exception) {
                when (ex) {
                    is FileNotFoundException -> throw stepConfig.exception(XProcError.xdDoesNotExist(UriUtils.path(absURI), ex.message ?: "???"), ex)
                    is IOException -> throw stepConfig.exception(XProcError.xdIsNotReadable(UriUtils.path(absURI), ex.message ?: "???"), ex)
                    else -> throw ex
                }
            }
        }

        if (absURI.scheme == "http" || absURI.scheme == "https") {
            val start = System.nanoTime()
            val req = InternetProtocolRequest(stepConfig, absURI)
            req.overrideContentType = documentProperties.contentType
            val resp = req.execute("GET")

            if (resp.statusCode >= 500) {
                throw stepConfig.exception(
                    XProcError.xdIsNotReadable(
                        absURI.toString(),
                        "HTTP response code: ${resp.statusCode}"
                    )
                )
            }
            if (resp.statusCode >= 400) {
                throw stepConfig.exception(
                    XProcError.xdDoesNotExist(
                        absURI.toString(),
                        "HTTP response code: ${resp.statusCode}"
                    )
                )
            }
            if (resp.response.size != 1) {
                throw stepConfig.exception(XProcError.xiDocumentReturnedMultipart())
            }
            val end = System.nanoTime()

            for (monitor in stepConfig.environment.monitors) {
                if (monitor is TraceListener) {
                    monitor.getResource(end - start, resp.responseUri, originalURI ?: absURI, false, false)
                }
            }

            return resp.response.first()
        }

        throw stepConfig.exception(XProcError.xdIsNotReadable(href.toString(), "Unsupported scheme."))
    }

    private fun loadFile(): XProcDocument {
        val start = System.nanoTime()
        val file = File(UriUtils.path(absURI))

        mediaType = if (documentProperties.has(Ns.contentType)) {
            MediaType.parse(documentProperties[Ns.contentType]!!.underlyingValue.stringValue)
        } else {
            val fileMediaType = stepConfig.documentManager.mimetypesFileTypeMap.getContentType(absURI.toString())
            MediaType.parse(fileMediaType)
        }

        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType

        if (!properties.has(Ns.baseUri)) {
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
        val doc = load(absURI, stream, mediaType)
        stream.close()
        val end = System.nanoTime()

        for (monitor in stepConfig.environment.monitors) {
            if (monitor is TraceListener) {
                monitor.getResource(end - start, absURI, originalURI, false, false)
            }
        }

        return doc
    }

    fun load(stream: InputStream, mediaType: MediaType, charset: Charset? = null): XProcDocument {
        return load(href, stream, mediaType, charset)
    }

    private fun load(uri: URI?, stream: InputStream, overrideMediaType: MediaType, charset: Charset? = null): XProcDocument {
        if (contentTypeLoaders == null) {
            val list = mutableListOf<ContentTypeLoader>()
            list.add(RdfLoader())
            for (provider in ContentTypeLoaderServiceProvider.providers()) {
                list.add(provider.create())
            }
            contentTypeLoaders = list
        }

        for (loader in contentTypeLoaders!!) {
            if (overrideMediaType in loader.contentTypes()) {
                return loader.load(stepConfig, uri, stream, overrideMediaType, mediaType.charset() ?: charset)
            }
        }

        mediaType = overrideMediaType
        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType
        if (href != null && !properties.has(Ns.baseUri)) {
            properties[Ns.baseUri] = href
        }

        val classification = mediaType.classification()
        val doc = when (classification) {
            MediaClassification.XML, MediaClassification.XHTML -> {
                try {
                    loadXml(uri, stream)
                } catch (ex: SaxonApiException) {
                    if (href != null) {
                        throw stepConfig.exception(XProcError.xdNotWellFormed(href), ex)
                    }
                    throw stepConfig.exception(XProcError.xdNotWellFormed(), ex)
                }
            }
            MediaClassification.HTML -> loadHtml(uri, stream)
            MediaClassification.JSON -> loadJson(stream)
            MediaClassification.TEXT -> loadText(stream, charset)
            // I'm not sure what to do with CSV. Maybe wait for XPath 4?
            // MediaType.CSV -> loadCsv(stream)
            MediaClassification.YAML -> loadYaml(stream)
            MediaClassification.TOML -> loadToml(stream)
            else -> loadBinary(stream)
        }

        return doc
    }

    private fun loadXml(uri: URI?, stream: InputStream): XProcDocument {
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

        val validating = if (parameters[Ns.dtdValidate] != null) {
            val value = parameters[Ns.dtdValidate]!!.underlyingValue
            if (value is BooleanValue) {
                value.booleanValue
            } else {
                stepConfig.typeUtils.parseBoolean(value.stringValue)
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
                    throw stepConfig.exception(XProcError.xdNotDtdValid(errorHandler.message ?: "No message provided"))
                }
                if (href != null) {
                    throw stepConfig.exception(XProcError.xdNotWellFormed(href))
                }
                throw stepConfig.exception(XProcError.xdNotWellFormed())
            }
            return XProcDocument.ofXml(xdm, stepConfig, properties)
        } finally {
            stepConfig.saxonConfig.configuration.parseOptions = saveParseOptions
        }
    }

    private fun loadHtml(uri: URI?, stream: InputStream): XProcDocument {
        val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
        val html = htmlBuilder.parse(stream)
        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        if (uri != null) {
            builder.baseURI = uri
        }
        val xdm = builder.build(DOMSource(html))
        return XProcDocument.ofXml(xdm, stepConfig, properties)
    }

    private fun loadJson(stream: InputStream): XProcDocument {
        val compiler = stepConfig.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        compiler.declareVariable(QName("opt"))
        val selector = compiler.compile("parse-json(\$a, \$opt)").load()
        selector.resourceResolver = stepConfig.environment.documentManager
        val inputjson = loadTextData(stream, StandardCharsets.UTF_8)
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
            return XProcDocument(json, stepConfig, properties)
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
                throw stepConfig.exception(XProcError.xdNotWellFormedJson(inputjson), ex)
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

    private fun loadText(stream: InputStream, charset: Charset?): XProcDocument {
        val builder = SaxonTreeBuilder(stepConfig.processor)
        builder.startDocument(stepConfig.baseUri)
        builder.addText(loadTextData(stream, charset))
        builder.endDocument()
        return XProcDocument.ofXml(builder.result, stepConfig, properties)
    }

    private fun loadBinary(stream: InputStream): XProcDocument {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        return XProcDocument.ofBinary(baos.toByteArray(), stepConfig, properties)
    }

    private fun loadTextData(stream: InputStream, inputCharset: Charset?): String {
        val suppliedCharset =  mediaType.charset() ?: inputCharset
        return readTextStream(stream, suppliedCharset)
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