package com.xmlcalabash.config

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import com.xmlcalabash.util.DefaultMessagePrinter
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import com.xmlcalabash.util.NopPagedMediaProvider
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.Verbosity
import com.xmlcalabash.visualizers.Detail
import com.xmlcalabash.visualizers.Plain
import com.xmlcalabash.visualizers.Silent
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

class ConfigurationLoader private constructor(private val config: XmlCalabashConfiguration) {
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
        private val _count = QName("count")
        private val _cssFormatter = QName("css-formatter")
        private val _dot = QName("dot")
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

        fun load(source: File): XmlCalabashConfiguration {
            return load(source, DefaultXmlCalabashConfiguration())
        }

        fun load(source: File, config: XmlCalabashConfiguration): XmlCalabashConfiguration {
            val isrc = InputSource(FileInputStream(source))
            isrc.systemId = source.toURI().toString()
            return load(isrc, config)
        }

        fun load(source: InputSource): XmlCalabashConfiguration {
            return load(source, DefaultXmlCalabashConfiguration())
        }

        fun load(source: InputSource, config: XmlCalabashConfiguration): XmlCalabashConfiguration {
            val loader = ConfigurationLoader(config)
            return loader.load(source)
        }
    }

    private lateinit var configFile: String
    private val uninitializedFormatters = mutableSetOf<URI>()
    private fun load(source: InputSource): XmlCalabashConfiguration {
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

        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            val manager = provider.create()
            managers.add(manager)
            uninitializedFormatters.addAll(manager.formatters())
        }
        config.pagedMediaManagers = managers

        val config = parse(root)

        for (manager in managers) {
            for (formatter in manager.formatters()) {
                if (formatter in uninitializedFormatters) {
                    manager.configure(formatter, emptyMap())
                }
            }
        }

        return config
    }

    private fun parse(root: XdmNode): XmlCalabashConfiguration {
        val saxonConfig = root.getAttributeValue(_saxonConfiguration)
        if (saxonConfig != null) {
            config.saxonConfigurationFile = File(saxonConfig)
            if (!config.saxonConfigurationFile!!.exists()) {
                throw XProcError.xiConfigurationInvalid(configFile, "file does not exist: ${saxonConfig}").exception()
            }
        }

        if (root.getAttributeValue(_mpt) != null) {
            try {
                config.mpt = root.getAttributeValue(_mpt).toDouble()
            } catch (_: NumberFormatException) {
                // nevermind, it's not important
                logger.debug { "mpt must be a number: ${root.getAttributeValue(_mpt)}"}
            }
        }

        if (root.getAttributeValue(_licensed) != null) {
            config.licensed = booleanAttribute(root.getAttributeValue(_licensed), "licensed")
        }

        config.pipe = booleanAttribute(root.getAttributeValue(_piped_io), "piped-io")
        config.verbosity = verbosityAttribute(root.getAttributeValue(_verbosity))

        // If this is Windows, assume the console output is in Windows CP 1252
        if (System.getProperty("os.name")?.lowercase()?.startsWith("windows") == true) {
            config.consoleEncoding = "windows-1252"
        }

        if (root.getAttributeValue(_console_output_encoding) != null) {
            val encoding = root.getAttributeValue(_console_output_encoding)
            try {
                if (Charset.isSupported(encoding)) {
                    config.consoleEncoding = encoding
                } else {
                    logger.warn { "Console encoding is not supported: ${encoding}, using ${config.consoleEncoding}" }
                }
            } catch (_: IOException) {
                logger.warn { "Console encoding is not supported: ${encoding}, using ${config.consoleEncoding}" }
            }
        }

        if (config.consoleEncoding.lowercase() != XmlCalabashConfiguration.DEFAULT_CONSOLE_ENCODING) {
            config.messagePrinter = DefaultMessagePrinter(config.consoleEncoding)
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
                        else -> throw XProcError.xiUnrecognizedConfigurationProperty(child.nodeName).exception()
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

        return config
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
        val proxies = mutableMapOf<String,String>()
        proxies.putAll(config.proxies)
        proxies[node.getAttributeValue(_scheme)!!] = node.getAttributeValue(_uri)!!
        config.proxies = proxies
    }

    private fun parseThreading(node: XdmNode) {
        checkAttributes(node, listOf(_count))
        try {
            config.threadPoolSize = node.getAttributeValue(_count)!!.toInt()
        } catch (_: NumberFormatException) {
            throw XProcError.xiInvalidSaxonConfigurationProperty("cc:threading", node.getAttributeValue(_count)!!).exception()
        }
    }

    private fun parseInline(node: XdmNode) {
        checkAttributes(node, listOf(_trimWhitespace))
        val value = node.getAttributeValue(_trimWhitespace)!!
        if (value == "true" || value == "false") {
            config.inlineTrimWhitespace = value == "true"
        } else {
            throw XProcError.xiUnrecognizedConfigurationValue(node.nodeName, _trimWhitespace, value).exception()
        }
    }

    private fun parseGraphviz(node: XdmNode) {
        checkAttributes(node, listOf(_dot))
        val dot = File(node.getAttributeValue(_dot)!!)
        if (!dot.exists() || dot.isDirectory) {
            throw XProcError.xiCannotFindGraphviz(dot.absolutePath).exception()
        }
        if (!dot.canExecute()) {
            throw XProcError.xiCannotExecuteGraphviz(dot.absolutePath).exception()
        }
        config.graphviz = dot
    }

    private fun parseSaxonConfigurationProperty(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name, _value))

        val properties = mutableMapOf<String,String>()
        properties.putAll(config.saxonConfigurationProperties)

        val key = node.getAttributeValue(Ns.name)!!
        val value = node.getAttributeValue(_value)!!
        val data = FeatureIndex.getData(key) ?: throw XProcError.xiUnrecognizedSaxonConfigurationProperty(key).exception()
        if (data.type == Boolean::class.java) {
            if (value == "true" || value == "false") {
                properties[key] = value
            } else {
                throw XProcError.xiInvalidSaxonConfigurationProperty(key, value).exception()
            }
        } else {
            properties[key] = value
        }

        config.saxonConfigurationProperties = properties
    }

    private fun parseSerialization(node: XdmNode) {
        val ctype = node.getAttributeValue(Ns.contentType)
        if (ctype == null) {
            throw XProcError.xiMissingConfigurationAttribute(node.nodeName, Ns.contentType).exception()
        }
        val map = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName == Ns.contentType) {
                continue
            }
            map[attr.nodeName] = attr.stringValue
        }

        val serialization = mutableMapOf<MediaType, Map<QName, String>>()
        serialization.putAll(config.serialization)
        serialization[MediaType.parse(ctype)] = map

        config.serialization = serialization
    }

    private fun parseMimetype(node: XdmNode) {
        checkAttributes(node, listOf(Ns.contentType, _extensions))

        val mimetypes = mutableMapOf<String, String>()
        mimetypes.putAll(config.mimetypes)
        mimetypes[node.getAttributeValue(Ns.contentType)!!] = node.getAttributeValue(_extensions)!!

        config.mimetypes = mimetypes
    }

    private fun parseSendmail(node: XdmNode) {
        checkAttributes(node, listOf(_host), listOf(_port, _username, _password))

        val sendmail = mutableMapOf<String, String>()
        sendmail.putAll(config.sendmail)

        for (attr in listOf(_host, _port, _username, _password)) {
            val value = node.getAttributeValue(_host)
            if (value != null) {
                sendmail[attr.localName] = value
            }
        }

        config.sendmail = sendmail
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
            val processors = mutableListOf<URI>()
            processors.addAll(config.pagedMediaCssProcessors)
            processors.add(cssFormatter)
            config.pagedMediaCssProcessors = processors

            if (cssFormatter == NopPagedMediaProvider.genericCssFormatter) {
                if (properties.isNotEmpty()) {
                    throw XProcError.xiForbiddenConfigurationAttributes(cssFormatter).exception()
                }
            } else {
                uninitializedFormatters.remove(cssFormatter)
                for (manager in config.pagedMediaManagers) {
                    if (manager.formatterSupported(cssFormatter)) {
                        manager.configure(cssFormatter, properties)
                    }
                }
            }
        }

        if (xslFormatter != null) {
            val processors = mutableListOf<URI>()
            processors.addAll(config.pagedMediaXslProcessors)
            processors.add(xslFormatter)
            config.pagedMediaXslProcessors = processors

            if (xslFormatter == NopPagedMediaProvider.genericXslFormatter) {
                if (properties.isNotEmpty()) {
                    throw XProcError.xiForbiddenConfigurationAttributes(xslFormatter).exception()
                }
            } else {
                uninitializedFormatters.remove(xslFormatter)
                for (manager in config.pagedMediaManagers) {
                    if (manager.formatterSupported(xslFormatter)) {
                        manager.configure(xslFormatter, properties)
                    }
                }
            }
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
        node.getAttributeValue(_bufferSize)?.let { config.messageBufferSize = it.toInt() }
    }

    private fun parseVisualizer(node: XdmNode) {
        val value = node.getAttributeValue(Ns.name)
        val options = mutableMapOf<String, String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName != Ns.name && attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                options[attr.nodeName.localName] = value
            }
        }

        val printer = config
        when (value) {
            null -> Unit
            "silent" -> config.visualizer = Silent(options)
            "plain" -> config.visualizer = Plain(config.messagePrinter, options)
            "detail" -> config.visualizer = Detail(config.messagePrinter, options)
            else -> throw XProcError.xiUnrecognizedConfigurationValue(ccVisualizer, Ns.name, value).exception()
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