package com.xmlcalabash.steps

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.MarkdownConfigurer
import com.xmlcalabash.util.MarkdownExtensions
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger

open class MarkdownToHtml(): AbstractAtomicStep() {
    companion object {
        private val q_extensions = QName(NsCx.namespace, "cx:extensions")
        private val q_configurers = QName(NsCx.namespace,"cx:configurers")
    }

    lateinit var document: XProcDocument
    private val markdownExtensions = MarkdownExtensions()

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val parameters = qnameMapBinding(Ns.parameters)
        val extensions = parameters[q_extensions]
        if (extensions != null) {
            configureExtensions(extensions)
        }

        val configurers = parameters[q_configurers]
        if (configurers != null) {
            if (configurers is XdmMap) {
                for (key in configurers.keySet()) {
                    val param = key.underlyingValue.stringValue
                    val className = configurers[key].underlyingValue.stringValue
                    try {
                        val klass = Class.forName(className)
                        val configurer = klass.getConstructor().newInstance() as MarkdownConfigurer
                        configurer.configure(markdownExtensions.options, param)
                    } catch (ex: Exception) {
                        logger.warn { "Failed to instantiate markdown configurer: ${param}: ${ex}" }
                    }
                }
            } else {
                logger.warn { "Configurers must be a map"}
            }
        }

        val options = markdownExtensions.options.toImmutable()

        val parser = Parser.builder(options).build()
        val html = parser.parse(document.value.underlyingValue.stringValue)
        val renderer = HtmlRenderer.builder(options).build()

        // We rely on the fact that the markdown parser returns well-formed markup consisting
        // of the paragraphs and other bits that would occur inside a <body> element and
        // that it returns them with no namespace declarations.
        val markup = "<body xmlns='http://www.w3.org/1999/xhtml'>" + renderer.render(html) + "</body>"

        val loader = DocumentLoader(stepConfig, document.baseURI, DocumentProperties(), mapOf())
        val result = loader.load(document.baseURI, markup.byteInputStream(), MediaType.HTML)

        receiver.output("result", result)
    }

    private fun configureExtensions(mapValue: XdmValue) {
        if (mapValue is XdmMap) {
            for (key in mapValue.keySet()) {
                val extension = key.stringValue
                val value = mapValue.get(extension)
                if (markdownExtensions.extensions.contains(extension)) {
                    if (value !is XdmMap) {
                        logger.warn { "Extension \"${extension}\" value must be a map" }
                    } else {
                        markdownExtensions.enable(markdownExtensions.extensions[extension]!!())
                        val options = markdownExtensions.extensionOptions[extension]!!
                        for (optkey in value.keySet()) {
                            val optname = optkey.stringValue
                            if (options.containsKey(optname)) {
                                configureExtension(extension, optname, value.get(optkey), options[optname]!!)
                            } else {
                                logger.warn { "Extension \"${extension}\" has no property named \"${optname}\"" }
                            }
                        }
                    }
                } else {
                    logger.warn { "No extension configured for \"${extension}\"" }
                }
            }
        }
    }

    private fun configureExtension(extension: String, property: String, optvalue: XdmValue, options: MarkdownExtensions.DataKeyOption) {
        val strvalue = when (optvalue) {
            is XdmMap -> ""
            is XdmArray -> ""
            else -> optvalue.underlyingValue.stringValue
        }

        when (options) {
            is MarkdownExtensions.DataKeyEnumerationOption -> {
                val dkenum = options.mapping[strvalue]
                if (dkenum != null) {
                    dkenum.set()
                } else {
                    logger.warn { "Extension \"${extension}\" property \"${property}\" has no value \"${strvalue}\"" }
                }
            }
            is MarkdownExtensions.DataKeyString -> {
                options.set(strvalue)
            }
            is MarkdownExtensions.DataKeyNullableString -> {
                if (optvalue === XdmEmptySequence.getInstance()) {
                    options.set(null)
                } else {
                    options.set(strvalue)
                }
            }
            is MarkdownExtensions.DataKeyInteger -> {
                try {
                    options.set(strvalue.toInt())
                } catch (ex: NumberFormatException) {
                    logger.warn { "Extension \"${extension}\" property \"${property}\" is not a number" }
                }
            }
            is MarkdownExtensions.DataKeyNullableInteger -> {
                if (optvalue === XdmEmptySequence.getInstance()) {
                    options.set(null)
                } else {
                    try {
                        options.set(strvalue.toInt())
                    } catch (ex: NumberFormatException) {
                        logger.warn { "Extension \"${extension}\" property \"${property}\" is not a number" }
                    }
                }
            }
            is MarkdownExtensions.DataKeyBoolean -> {
                when (strvalue) {
                    "true", "1", "yes" -> options.set(true)
                    "false", "0", "no" -> options.set(false)
                    else -> {
                        logger.warn { "Extension \"${extension}\" property \"${property}\" must be a boolean: ${strvalue}"}
                    }
                }
            }
            is MarkdownExtensions.DataKeyMapStringString -> {
                val map = mutableMapOf<String, String>()
                if (optvalue is XdmMap) {
                    for (key in optvalue.keySet()) {
                        map[key.underlyingValue.stringValue] = optvalue.get(key).underlyingValue.stringValue
                    }
                    options.set(map)
                } else {
                    logger.warn { "Extension \"${extension}\" property \"${property}\" must be a map(xs:string,xs:string)" }
                }
            }
            is MarkdownExtensions.DataKeyMapCharacterInteger -> {
                val map = mutableMapOf<Char, Int>()
                if (optvalue is XdmMap) {
                    for (key in optvalue.keySet()) {
                        val kchar = key.underlyingValue.stringValue
                        val kint = optvalue.get(key).underlyingValue.stringValue
                        if (kchar.length == 1) {
                            try {
                                map[kchar[0]] = kint.toInt()
                            } catch (ex: NumberFormatException) {
                                logger.warn { "Extension \"${extension}\" property \"${property}\" values must be integers" }
                            }
                        } else {
                            logger.warn { "Extension \"${extension}\" property \"${property}\" keys must be single characters" }
                        }
                    }
                    options.set(map)
                } else {
                    logger.warn { "Extension \"${extension}\" property \"${property}\" must be a map(xs:string,xs:integer)" }
                }
            }
            is MarkdownExtensions.DataKeyArrayString -> {
                val vlist = mutableListOf<String>()
                val iter = optvalue.iterator()
                while (iter.hasNext()) {
                    vlist.add(iter.next().stringValue)
                }
                options.set(vlist.toTypedArray())
            }
            else -> {
                logger.warn { "Internal error: unexpected option type: ${options}" }
            }
        }
    }

    override fun toString(): String = "p:markdown-to-html"
}