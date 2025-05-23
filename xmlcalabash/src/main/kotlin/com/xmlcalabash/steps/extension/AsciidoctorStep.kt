package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.DateTimeValue
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.CompatMode
import org.asciidoctor.Options
import org.asciidoctor.Placement
import org.asciidoctor.SafeMode
import org.asciidoctor.log.LogHandler
import org.asciidoctor.log.LogRecord
import org.asciidoctor.log.Severity
import org.jruby.RubyObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Date

class AsciidoctorStep(): AbstractAtomicStep() {
    companion object {
        val _backend = QName("backend")
    }

    var standalone = false
    val params = mutableMapOf<QName, XdmValue>()
    lateinit var attributes: Map<QName, XdmValue>

    override fun run() {
        super.run()

        val text = queues["source"]!!.first()
        params.putAll(qnameMapBinding(Ns.parameters))
        attributes = qnameMapBinding(Ns.attributes)
        val backend = stringBinding(_backend)
            ?: params[_backend]?.underlyingValue?.stringValue
            ?: attributes[_backend]?.underlyingValue?.stringValue

        if (backend == null) {
            params[_backend] = XdmAtomicValue("docbook")
        }

        if (backend != null) {
            val pbackend = params[_backend]
            if (pbackend != null && backend != pbackend.underlyingValue.stringValue) {
                throw IllegalArgumentException("Conflicting backends specified")
            }
            val abackend = attributes[_backend]
            if (abackend != null && backend != abackend.underlyingValue.stringValue) {
                throw IllegalArgumentException("Conflicting backends specified")
            }
        }

        val options = options()
        val asciidoctor = Asciidoctor.Factory.create()
        asciidoctor.registerLogHandler(AsciidoctorLogHandler())

        if (backend == "pdf") {
            convertPdf(text, asciidoctor, options)
        } else {
            convertMarkup(text, asciidoctor, options,
                params[_backend]?.underlyingValue?.stringValue ?: backend!!)
        }
    }

    private fun convertMarkup(text: XProcDocument, asciidoctor: Asciidoctor, options: Options, backend: String) {
        val namespace = if (backend == "docbook") {
            "http://docbook.org/ns/docbook"
        } else {
            "http://www.w3.org/1999/xhtml"
        }

        val contentType = if (backend == "docbook") {
            MediaType.XML
        } else {
            MediaType.HTML
        }

        if (standalone) {
            val markup = asciidoctor.convert(text.value.underlyingValue.stringValue, options)
            val loader = DocumentLoader(stepConfig, text.baseURI)
            val bais = ByteArrayInputStream(markup.toByteArray(StandardCharsets.UTF_8))
            val doc = loader.load(bais, contentType)
            receiver.output("result", doc)
        } else {
            val markup = "<DOC xmlns='${namespace}' xmlns:xl='http://www.w3.org/1999/xlink'>" +
                    "${asciidoctor.convert(text.value.underlyingValue.stringValue, options)}</DOC>"
            val loader = DocumentLoader(stepConfig, text.baseURI)
            val bais = ByteArrayInputStream(markup.toByteArray(StandardCharsets.UTF_8))
            val doc = loader.load(bais, MediaType.XML)

            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(null)

            for (child in (doc.value as XdmNode).axisIterator(Axis.CHILD)) {
                if (child.nodeKind == XdmNodeKind.ELEMENT && child.nodeName.localName == "DOC") {
                    for (gchild in child.axisIterator(Axis.CHILD)) {
                        builder.addSubtree(gchild)
                    }
                }
            }

            builder.endDocument()
            val result = builder.result
            receiver.output("result", XProcDocument.ofXml(result, text.context, contentType))
        }
    }

    private fun convertPdf(text: XProcDocument, asciidoctor: Asciidoctor, options: Options) {
        // If there's a practical API for converting to PDF without using files, I couldn't find it.
        val tempAsciidoctor = Files.createTempFile("asciidoctor", ".adoc")
        tempAsciidoctor.toFile().deleteOnExit()

        val stream = FileOutputStream(tempAsciidoctor.toFile())
        val bytes = text.value.underlyingValue.stringValue.toByteArray(StandardCharsets.UTF_8)
        stream.write(bytes)
        stream.close()

        options.setToFile(true)
        options.setSafe(SafeMode.UNSAFE)

        val adocFile = tempAsciidoctor.toFile()
        val pdfFile = File(adocFile.parentFile, adocFile.nameWithoutExtension + ".pdf")
        pdfFile.deleteOnExit()
        asciidoctor.convertFile(adocFile, options)

        val pdfstream = FileInputStream(pdfFile)
        val pdfBytes = pdfstream.readAllBytes()
        pdfstream.close()

        tempAsciidoctor.toFile().delete()
        pdfFile.delete()

        receiver.output("result", XProcBinaryDocument(pdfBytes, text.context).with(MediaType.PDF, true))
    }

    private fun options(): Options {
        val builder = Options.builder()
        builder.toFile(false)
        builder.parse(false)

        for ((key, value) in params) {
            if (key.namespaceUri != NamespaceUri.NULL) {
                continue
            }

            val param = key.localName
            val svalue = value.underlyingValue.stringValue
            val bvalue = svalue != "false"
            when (param) {
                Options.BACKEND -> {
                    builder.backend(svalue)
                }
                Options.DOCTYPE -> {
                    builder.docType(svalue)
                }
                Options.STANDALONE -> {
                    standalone = bvalue
                    builder.standalone(standalone)
                }
                Options.TEMPLATE_DIRS -> {
                    val files = mutableListOf<File>()
                    for (file in value.iterator()) {
                        files.add(File(file.underlyingValue.stringValue))
                    }
                    builder.templateDirs(*files.toTypedArray())
                }
                Options.TEMPLATE_ENGINE -> {
                    builder.templateEngine(svalue)
                }
                Options.TEMPLATE_CACHE -> {
                    builder.templateCache(bvalue)
                }
                Options.SAFE -> {
                    val mode = svalue.lowercase()
                    when (mode) {
                        "unsafe" -> builder.safe(SafeMode.UNSAFE)
                        "safe" -> builder.safe(SafeMode.SAFE)
                        "server" -> builder.safe(SafeMode.SERVER)
                        "secure" -> builder.safe(SafeMode.SECURE)
                        else -> {
                            throw IllegalArgumentException("Unexpected cx:asciidoctor safe mode: $mode")
                        }
                    }
                }
                Options.SOURCEMAP -> {
                    builder.sourcemap(bvalue)
                }
                Options.ERUBY -> {
                    builder.eruby(svalue)
                }
                Options.COMPACT -> {
                    builder.compact(bvalue)
                }
                Options.PARSE_HEADER_ONLY -> {
                    builder.parseHeaderOnly(bvalue)
                }
                else -> {
                    stepConfig.debug { "The cx:asciidoctor step ignores the ${param} option." }
                }
            }
        }

        val attributes = attributes()
        builder.attributes(attributes)
        return builder.build()
    }

    private fun attributes(): Attributes {
        val builder = Attributes.builder()
        builder.unsetStyleSheet()
        builder.cacheUri(false)

        for ((key, value) in attributes) {
            if (key.namespaceUri != NamespaceUri.NULL) {
                continue
            }

            val name = key.localName
            val svalue = value.underlyingValue.stringValue
            val bvalue = svalue != "false"
            when (name) {
                Attributes.ALLOW_URI_READ -> {
                    builder.allowUriRead(bvalue)
                }
                Attributes.APPENDIX_CAPTION -> {
                    builder.appendixCaption(svalue)
                }
                Attributes.ATTRIBUTE_MISSING -> {
                    builder.attributeMissing(svalue)
                }
                Attributes.ATTRIBUTE_UNDEFINED -> {
                    builder.attributeUndefined(svalue)
                }
                Attributes.BACKEND -> {
                    builder.backend(svalue)
                }
                Attributes.COMPAT_MODE -> {
                    val mode = svalue.lowercase()
                    when (mode) {
                        "default" -> builder.compatMode(CompatMode.DEFAULT)
                        "legacy" -> builder.compatMode(CompatMode.LEGACY)
                        else -> {
                            throw IllegalArgumentException("Unexpected cx:asciidoctor compat mode: $mode")
                        }
                    }
                }
                Attributes.DATA_URI -> {
                    builder.dataUri(bvalue)
                }
                Attributes.DOCDATE -> {
                    if (value.underlyingValue is DateTimeValue) {
                        val date = value.underlyingValue as DateTimeValue
                        builder.docDate(Date.from(date.toJavaInstant()))
                    } else {
                        throw IllegalArgumentException("Doc date must be an xs:dateTime")
                    }
                }
                Attributes.DOCTIME -> {
                    if (value.underlyingValue is DateTimeValue) {
                        val date = value.underlyingValue as DateTimeValue
                        builder.docTime(Date.from(date.toJavaInstant()))
                    } else {
                        throw IllegalArgumentException("Doc time must be an xs:dateTime")
                    }
                }
                Attributes.DOCTYPE -> {
                    builder.docType(svalue)
                }
                Attributes.EXPERIMENTAL -> {
                    builder.experimental(bvalue)
                }
                Attributes.HARDBREAKS -> {
                    builder.hardbreaks(bvalue)
                }
                Attributes.HIDE_URI_SCHEME -> {
                    builder.hiddenUriScheme(bvalue)
                }
                Attributes.ICONFONT_CDN -> {
                    builder.iconFontCdn(URI(svalue))
                }
                Attributes.ICONFONT_NAME -> {
                    builder.iconFontName(svalue)
                }
                Attributes.ICONFONT_REMOTE -> {
                    builder.iconFontRemote(bvalue)
                }
                Attributes.ICONS -> {
                    builder.icons(svalue)
                }
                Attributes.ICONS_DIR -> {
                    builder.iconsDir(svalue)
                }
                Attributes.IGNORE_UNDEFINED -> {
                    builder.ignoreUndefinedAttributes(bvalue)
                }
                Attributes.IMAGESDIR -> {
                    builder.imagesDir(svalue)
                }
                Attributes.LINK_ATTRS -> {
                    builder.linkAttrs(bvalue)
                }
                Attributes.LINK_CSS -> {
                    builder.linkCss(bvalue)
                }
                Attributes.LOCALDATE -> {
                    if (value.underlyingValue is DateTimeValue) {
                        val date = value.underlyingValue as DateTimeValue
                        builder.localDate(Date.from(date.toJavaInstant()))
                    } else {
                        throw IllegalArgumentException("Local date must be an xs:dateTime")
                    }
                }
                Attributes.LOCALTIME -> {
                    if (value.underlyingValue is DateTimeValue) {
                        val date = value.underlyingValue as DateTimeValue
                        builder.localTime(Date.from(date.toJavaInstant()))
                    } else {
                        throw IllegalArgumentException("Local time must be an xs:dateTime")
                    }
                }
                Attributes.MATH -> {
                    builder.math(svalue)
                }
                Attributes.MAX_INCLUDE_DEPTH -> {
                    builder.maxIncludeDepth(svalue.toInt())
                }
                Attributes.NO_FOOTER -> {
                    builder.noFooter(bvalue)
                }
                Attributes.SECTION_NUMBERS -> {
                    builder.sectionNumbers(bvalue)
                }
                Attributes.SECT_NUM_LEVELS -> {
                    builder.sectNumLevels(svalue.toInt())
                }
                Attributes.SET_ANCHORS -> {
                    builder.setAnchors(bvalue)
                }
                Attributes.SHOW_TITLE -> {
                    builder.showTitle(bvalue)
                }
                Attributes.SKIP_FRONT_MATTER -> {
                    builder.skipFrontMatter(bvalue)
                }
                Attributes.SOURCE_HIGHLIGHTER -> {
                    builder.sourceHighlighter(svalue)
                }
                Attributes.SOURCE_LANGUAGE -> {
                    builder.sourceLanguage(svalue)
                }
                Attributes.STYLESHEET_NAME -> {
                    builder.styleSheetName(svalue)
                }
                Attributes.STYLES_DIR -> {
                    builder.stylesDir(svalue)
                }
                Attributes.TITLE -> {
                    builder.title(svalue)
                }
                Attributes.TOC -> {
                    when (svalue.lowercase()) {
                        "true" -> builder.tableOfContents(true)
                        "false" -> builder.tableOfContents(false)
                        "top" -> builder.tableOfContents(Placement.TOP)
                        "bottom" -> builder.tableOfContents(Placement.BOTTOM)
                        "left" -> builder.tableOfContents(Placement.LEFT)
                        "right" -> builder.tableOfContents(Placement.RIGHT)
                        "preamble" -> builder.tableOfContents(Placement.PREAMBLE)
                        "macro" -> builder.tableOfContents(Placement.MACRO)
                        else -> {
                            throw IllegalArgumentException("Unexpected cx:asciidoctor TOC placement: $svalue")
                        }
                    }
                }
                Attributes.UNTITLED_LABEL -> {
                    builder.untitledLabel(svalue)
                }
                else -> {
                    stepConfig.debug { "The cx:asciidoctor step ignores the ${name} attribute." }
                }
            }
        }

        return builder.build()
    }

    override fun toString(): String = "cx:asciidoctor"

    inner class AsciidoctorLogHandler(): LogHandler {
        override fun log(logRecord: LogRecord?) {
            if (logRecord == null) {
                return
            }

            val sb = StringBuilder()
            sb.append("cx:asciidoctor")
            logRecord.cursor?.lineNumber?.let { sb.append(":${it}") }
            sb.append(": ${logRecord.message}")

            val reporter = stepConfig.environment.messageReporter
            when (logRecord.severity) {
                Severity.ERROR -> reporter.error { Report(Verbosity.ERROR, sb.toString()) }
                Severity.FATAL -> reporter.error { Report(Verbosity.ERROR, sb.toString()) }
                Severity.UNKNOWN -> reporter.error { Report(Verbosity.ERROR, sb.toString()) }
                Severity.WARN -> reporter.warn { Report(Verbosity.WARN, sb.toString()) }
                Severity.INFO -> reporter.info { Report(Verbosity.INFO, sb.toString()) }
                Severity.DEBUG -> reporter.debug { Report(Verbosity.DEBUG, sb.toString()) }
            }
        }
    }
}
