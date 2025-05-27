package com.xmlcalabash.config

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.*
import net.sf.saxon.lib.FeatureIndex
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.nio.charset.Charset
import javax.xml.transform.sax.SAXSource

class ConfigurationLoader(val builder: XmlCalabashBuilder) {
    companion object {
        private val ns = NamespaceUri.of("https://xmlcalabash.com/ns/configuration")
        private val ccXmlCalabash = QName(ns, "cc:xml-calabash")
        private val ccSystemProperty = QName(ns, "cc:system-property")
        private val ccProxy = QName(ns, "cc:proxy")
        private val ccThreading = QName(ns, "cc:threading")
        private val ccInline = QName(ns, "cc:inline")
        private val ccGraphviz = QName(ns, "cc:graphviz")
        private val ccSaxonConfigurationProperty = QName(ns, "cc:saxon-configuration-property")
        private val ccSerialization = QName(ns, "cc:serialization")
        private val ccMimetype = QName(ns, "cc:mimetype")
        private val ccSendmail = QName(ns, "cc:send-mail")
        private val ccPagedMedia = QName(ns, "cc:paged-media")
        private val ccVisualizer = QName(ns, "cc:visualizer")
        private val ccMessageReporter = QName(ns, "cc:message-reporter")
        private val ccXmlSchema = QName(ns, "cc:xml-schema")
        private val ccCatalog = QName(ns, "cc:catalog")

        private val _count = QName("count")
        private val _cssFormatter = QName("css-formatter")
        private val _dot = QName("dot")
        private val _style = QName("style")
        private val _extensions = QName("extensions")
        private val _host = QName("host")
        private val _licensed = QName("licensed")
        private val _mpt = QName("mpt")
        private val _password = QName("password")
        private val _port = QName("port")
        private val _piped_io = QName("piped-io")
        private val _console_output_encoding = QName("console-output-encoding")
        private val _saxonConfiguration = QName("saxon-configuration")
        private val _scheme = QName("scheme")
        private val _trimWhitespace = QName("trim-whitespace")
        private val _uri = QName("uri")
        private val _username = QName("username")
        private val _value = QName("value")
        private val _verbosity = QName("verbosity")
        private val _xslFormatter = QName("xsl-formatter")
        private val _bufferSize = QName("buffer-size")
    }

    private lateinit var configFile: String

    fun load(source: File) {
        logger.info { "Loading XML Calabash configuration ${source.absoluteFile}" }
        val isrc = InputSource(FileInputStream(source))
        isrc.systemId = source.toURI().toString()
        load(isrc)
    }

    fun load(source: URI) {
        logger.info { "Loading XML Calabash configuration ${source}" }
        val isrc = InputSource(source.toString())
        load(isrc)
    }

    fun load(source: InputSource) {
        configFile = source.systemId ?: ""

        val processor = Processor(false)
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val destination = XdmDestination()
        builder.parse(SAXSource(source), destination)

        val root = S9Api.documentElement(destination.xdmNode)
        if (root.nodeName != ccXmlCalabash) {
            throw XProcError.xiConfigurationInvalid(configFile).exception()
        }

        parse(root)
    }

    private fun parse(root: XdmNode) {
        checkAttributes(root, listOf(), listOf(
            _console_output_encoding, _licensed, _piped_io, _saxonConfiguration,
            Ns.tryNamespaces, Ns.useLocationHints, Ns.validationMode,
            _verbosity, Ns.version, _mpt))

        if ((root.getAttributeValue(Ns.version) ?: "1.0") != "1.0") {
            throw XProcError.xiInvalidConfigurationAttributeValue(root.nodeName, Ns.version, root.getAttributeValue(Ns.version)!!).exception()
        }

        val saxonConfig = root.getAttributeValue(_saxonConfiguration)
        if (saxonConfig != null) {
            val uri = UriUtils.resolve(root.baseURI, saxonConfig)!!
            builder.setSaxonConfigurationFile(File(UriUtils.path(uri)))
        }

        if (root.getAttributeValue(_mpt) != null) {
            try {
                builder._mpt = root.getAttributeValue(_mpt).toDouble()
            } catch (_: NumberFormatException) {
                // nevermind, it's not important
                logger.debug { "mpt must be a number: ${root.getAttributeValue(_mpt)}"}
            }
        }

        if (root.getAttributeValue(_licensed) != null) {
            builder.setLicensed(booleanAttribute(root.getAttributeValue(_licensed), "licensed"))
        }

        builder.setPipe(booleanAttribute(root.getAttributeValue(_piped_io), "piped-io"))
        builder.setVerbosity(verbosityAttribute(root.getAttributeValue(_verbosity)))

        when (root.getAttributeValue(Ns.validationMode)) {
            null -> Unit
            "strict" -> builder.setValidationMode(ValidationMode.STRICT)
            "lax" -> builder.setValidationMode(ValidationMode.LAX)
            else -> throw XProcError.xiConfigurationInvalid(configFile,
                "validation mode: ${root.getAttributeValue(Ns.validationMode)}").exception()
        }

        builder.setTryNamespaces(booleanAttribute(root.getAttributeValue(Ns.tryNamespaces), "try-namespaces"))
        builder.setUseLocationHints(booleanAttribute(root.getAttributeValue(Ns.useLocationHints), "use-location-hints"))

        // If this is Windows, assume the console output is in Windows CP 1252
        if (System.getProperty("os.name")?.lowercase()?.startsWith("windows") == true) {
            builder.setConsoleEncoding("windows-1252")
        }

        if (root.getAttributeValue(_console_output_encoding) != null) {
            val encoding = root.getAttributeValue(_console_output_encoding)
            try {
                if (Charset.isSupported(encoding)) {
                    builder.setConsoleEncoding(encoding)
                } else {
                    logger.warn { "Console encoding is not supported: ${encoding}" }
                }
            } catch (_: IOException) {
                logger.warn { "Console encoding is not supported: ${encoding}" }
            }
        }

        for (child in root.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.ELEMENT -> {
                    when (child.nodeName) {
                        ccSystemProperty -> parseSystemProperty(child)
                        ccProxy -> parseProxy(child)
                        ccThreading -> parseThreading(child)
                        ccInline -> parseInline(child)
                        ccGraphviz -> parseGraphviz(child)
                        ccSaxonConfigurationProperty -> parseSaxonConfigurationProperty(child)
                        ccSerialization -> parseSerialization(child)
                        ccMimetype -> parseMimetype(child)
                        ccSendmail -> parseSendmail(child)
                        ccPagedMedia -> parsePagedMedia(child)
                        ccMessageReporter -> parseMessageReporter(child)
                        ccVisualizer -> parseVisualizer(child)
                        ccXmlSchema -> parseXmlSchema(child)
                        ccCatalog -> parseCatalog(child)
                        else -> {
                            if (child.nodeName.namespaceUri == ns) {
                                throw XProcError.xiUnrecognizedConfigurationProperty(child.nodeName).exception()
                            }
                            parseOther(child)
                        }
                    }
                }
                XdmNodeKind.TEXT -> {
                    val text = child.underlyingValue.stringValue
                    if (text.isNotBlank()) {
                        throw XProcError.xiConfigurationInvalid(configFile, "text is not allowed: ${text}").exception()
                    }
                }
                else -> Unit
            }
        }
    }

    private fun booleanAttribute(value: String?, name: String): Boolean {
        if (value != null && value != "true" && value != "false") {
            throw XProcError.xiConfigurationInvalid(configFile, "invalid ${name} setting: ${value}").exception()
        }
        return value == "true"
    }

    private fun verbosityAttribute(value: String?): Verbosity {
        return when (value) {
            null -> Verbosity.INFO
            "error" -> Verbosity.ERROR
            "warn", "warning", "warnings" -> Verbosity.WARN
            "info" -> Verbosity.INFO
            "debug" -> Verbosity.DEBUG
            "trace" -> Verbosity.TRACE
            else -> throw XProcError.xiConfigurationInvalid(configFile, "invalid verbose setting: ${value}").exception()
        }
    }

    private fun parseSystemProperty(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name, _value))
        System.setProperty(node.getAttributeValue(Ns.name)!!, node.getAttributeValue(_value)!!)
    }

    private fun parseProxy(node: XdmNode) {
        checkAttributes(node, listOf(_scheme, _uri))
        builder.addProxy(node.getAttributeValue(_scheme)!!, node.getAttributeValue(_uri)!!)
    }

    private fun parseThreading(node: XdmNode) {
        checkAttributes(node, emptyList(), listOf(_count))
        try {
            builder.setMaxThreadCount( (node.getAttributeValue(_count)?.toInt() ?: Runtime.getRuntime().availableProcessors()) )
        } catch (_: NumberFormatException) {
            throw XProcError.xiInvalidSaxonConfigurationProperty("cc:threading", node.getAttributeValue(_count)!!).exception()
        }
    }

    private fun parseInline(node: XdmNode) {
        checkAttributes(node, listOf(_trimWhitespace))
        val value = node.getAttributeValue(_trimWhitespace)!!
        if (value == "true" || value == "false") {
            builder.setInlineTrimWhitespace(value == "true")
        } else {
            throw XProcError.xiUnrecognizedConfigurationValue(node.nodeName, _trimWhitespace, value).exception()
        }
    }

    private fun parseGraphviz(node: XdmNode) {
        checkAttributes(node, listOf(_dot), listOf(_style))
        val dot = File(node.getAttributeValue(_dot)!!)
        if (!dot.exists() || dot.isDirectory) {
            throw XProcError.xiCannotFindGraphviz(dot.absolutePath).exception()
        }
        if (!dot.canExecute()) {
            throw XProcError.xiCannotExecuteGraphviz(dot.absolutePath).exception()
        }
        builder.setGraphviz(dot)

        val style = node.getAttributeValue(_style)
        if (style != null) {
            builder.setGraphStyle(UriUtils.resolve(node.baseURI, style))
        }
    }

    private fun parseSaxonConfigurationProperty(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name, _value))

        val key = node.getAttributeValue(Ns.name)!!
        val value = node.getAttributeValue(_value)!!
        val data = FeatureIndex.getData(key) ?: throw XProcError.xiUnrecognizedSaxonConfigurationProperty(key).exception()
        if (data.type == Boolean::class.java) {
            if (value == "true" || value == "false") {
                builder.addSaxonConfigurationProperty(key, value)
            } else {
                throw XProcError.xiInvalidSaxonConfigurationProperty(key, value).exception()
            }
        } else {
            builder.addSaxonConfigurationProperty(key, value)
        }
    }

    private fun parseSerialization(node: XdmNode) {
        if (node.getAttributeValue(Ns.contentType) == null) {
            throw XProcError.xiMissingConfigurationAttribute(node.nodeName, Ns.contentType).exception()
        }
        val ctype = MediaType.parse(node.getAttributeValue(Ns.contentType))

        val map = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName == Ns.contentType) {
                continue
            }
            builder.addSerializationProperty(ctype, attr.nodeName, attr.stringValue)
        }
    }

    private fun parseMimetype(node: XdmNode) {
        checkAttributes(node, listOf(Ns.contentType, _extensions))
        val ctype = node.getAttributeValue(Ns.contentType)!!
        val ext = node.getAttributeValue(_extensions)!!
        builder.addMimeType(ctype, ext.split("\\s+".toRegex()))
    }

    private fun parseSendmail(node: XdmNode) {
        checkAttributes(node, listOf(_host), listOf(_port, _username, _password))

        for (attr in listOf(_host, _port, _username, _password)) {
            val value = node.getAttributeValue(attr)
            if (value != null) {
                builder.addSendmailProperty(attr.localName, value)
            }
        }
    }

    private fun parsePagedMedia(node: XdmNode) {
        var cssFormatter: URI? = null
        var xslFormatter: URI? = null
        val properties = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            when (attr.nodeName) {
                _cssFormatter -> cssFormatter = pagedMediaProcessorUri("css-formatter", attr.stringValue)
                _xslFormatter -> xslFormatter = pagedMediaProcessorUri("xsl-formatter", attr.stringValue)
                else -> properties[attr.nodeName] = attr.stringValue
            }
        }

        if (cssFormatter == null && xslFormatter == null) {
            throw XProcError.xiMissingConfigurationAttributes(node.nodeName, "at least one of xsl-formatter or css-formatter is required").exception()
        }

        if (cssFormatter != null) {
            builder.addPagedMediaCssProcessor(cssFormatter, properties)
        }

        if (xslFormatter != null) {
            builder.addPagedMediaXslProcessor(xslFormatter, properties)
        }
    }

    private fun pagedMediaProcessorUri(type: String, value: String?): URI? {
        if (value == null) {
            return null
        }
        if (value.contains("/")) {
            return URI.create(value)
        }
        return URI.create("https://xmlcalabash.com/paged-media/${type}/${value}")
    }

    private fun parseMessageReporter(node: XdmNode) {
        checkAttributes(node, listOf(_bufferSize))
        node.getAttributeValue(_bufferSize)?.let { builder.setMessageBufferSize(it.toInt()) }
    }

    private fun parseVisualizer(node: XdmNode) {
        val value = node.getAttributeValue(Ns.name)
        val options = mutableMapOf<String, String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName != Ns.name && attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                options[attr.nodeName.localName] = value
            }
        }

        if (value != null) {
            builder.setVisualizer(value, options)
        }
    }

    private fun parseOther(node: XdmNode) {
        val map = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            map[attr.nodeName] = attr.stringValue
        }
        builder.addOther(node.nodeName, map)
    }

    private fun parseXmlSchema(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))
        builder.addXmlSchemaDocument(UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!)
        if (node.children().firstOrNull() != null) {
            throw XProcError.xiConfigurationXmlSchemaElementMustBeEmpty().exception()
        }
    }

    private fun parseCatalog(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))
        builder.addXmlCatalog( UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!)
        if (node.children().firstOrNull() != null) {
            throw XProcError.xiConfigurationCatalogElementMustBeEmpty().exception()
        }
    }

    private fun checkAttributes(node: XdmNode, attributes: List<QName>, optionalAttributes: List<QName> = listOf()) {
        val seen = mutableSetOf<QName>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            seen.add(attr.nodeName)
            if (!attributes.contains(attr.nodeName) && !optionalAttributes.contains(attr.nodeName)) {
                throw XProcError.xiUnrecognizedConfigurationAttribute(node.nodeName, attr.nodeName).exception()
            }
        }
        for (attr in attributes) {
            if (!seen.contains(attr)) {
                throw XProcError.xiMissingConfigurationAttribute(node.nodeName, attr).exception()
            }
        }
    }
}