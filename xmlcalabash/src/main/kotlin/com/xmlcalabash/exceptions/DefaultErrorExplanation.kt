package com.xmlcalabash.exceptions

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.InternalDocumentResolver
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.*
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class DefaultErrorExplanation(val printer: MessagePrinter): ErrorExplanation {
    var showStackTrace = true

    companion object {
        private var loaded = false
        private val messages = mutableListOf<ErrorExplanationTemplate>()
    }

    init {
        if (!loaded) {
            val uc = System.getProperty("user.country", "")
            val ul = System.getProperty("user.language", "")
            if (uc != "" && ul != "") {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.${ul}_${uc}.txt")
                    ?.let { loadExplanations(it) }
            }

            if (!loaded && ul != "") {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.${ul}.txt")
                    ?.let { loadExplanations(it) }
            }

            if (!loaded) {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.txt")
                    ?.let { loadExplanations(it) }
            }

            loaded = true
        }
    }

    override fun message(error: XProcError, includeDetails: Boolean): String {
        val errorMessage = if (error.code.namespaceUri in listOf(NsErr.namespace, NsCx.errorNamespace)) {
            val message = template(error.code, error.variant, error.details.size).message
            substitute(message, *error.details)
        } else {
            "(no message for error)"
        }

        if (!includeDetails) {
            return errorMessage
        }

        val sb = StringBuilder()
        if (error.location != Location.NULL) {
            sb.append("Fatal ${error.code} at ${error.location}")
        } else {
            sb.append("Fatal ${error.code}")
        }

        sb.append(": ").append(errorMessage)

        if (error.inputLocation != Location.NULL) {
            sb.append("\n").append("   in ${error.inputLocation}")
        }

        if (error.throwable != null && error.throwable?.message != null) {
            sb.append("\n").append("   cause: ${error.throwable!!.toString()}")
        }

        for (detail in error.details) {
            if (detail is XProcDocument) {
                sb.append("\n")
                sb.append(showDetail(detail))
            }
        }

        return sb.toString()
    }

    override fun report(error: XProcError) {
        printer.println(message(error, true))
        if (showStackTrace) {
            printer.println("Stack trace:")
            var count = error.stackTrace.size
            for (frame in error.stackTrace) {
                if (frame.stepName.startsWith('!')) {
                    printer.println("\t[${count}] <${frame.stepType}>")
                } else {
                    printer.println("\t[${count}] <${frame.stepType} name=\"${frame.stepName}\">")
                }
                count--
            }
        }
    }

    override fun explanation(error: XProcError): String {
        val message = template(error.code, error.variant, error.details.size).explanation
        return substitute(message, *error.details)
    }

    override fun reportExplanation(error: XProcError) {
        printer.println(explanation(error))
    }

    private fun template(code: QName, variant: Int, count: Int): ErrorExplanationTemplate {
        val clark = code.clarkName
        val templates = messages.filter { it.code == clark }.filter { it.variant == variant }.filter { it.cardinality <= count }
        if (templates.isEmpty()) {
            return ErrorExplanationTemplate(clark, 1, "[No explanatory message for ${code}]", "[No explanation for ${code}]")
        } else {
            var maxCardinality = -1
            var maxTemplate: ErrorExplanationTemplate? = null
            for (template in templates) {
                if (template.cardinality == count) {
                    return template
                }
                if (template.cardinality > maxCardinality) {
                    maxCardinality = template.cardinality
                    maxTemplate = template
                }
            }
            return maxTemplate!!
        }
    }

    private fun substitute(text: String, vararg details: Any): String {
        // Multiline regex matching is fine, but groups only match up to the first newline ???
        val re = Regex("^(.*?)\\$(\\d+)(.*)$")
        val sb = StringBuilder()
        for ((index, line) in text.split("\n").iterator().withIndex()) {
            if (index > 0) {
                sb.append("\n")
            }
            var message = line
            var match = re.find(message)
            while (match != null) {
                val (preamble, token, postamble) = match.destructured
                val number = token.toInt() - 1
                if (number < details.size) {
                    message = "${preamble}${stringify(details[number])}${postamble}"
                } else {
                    message = "${preamble}${postamble}"
                }
                match = re.find(message)
            }
            sb.append(message)
        }
        return sb.toString()
    }

    private fun stringify(any: Any): String {
        when (any) {
            is List<*> -> {
                val sb = StringBuilder()
                sb.append("[")
                for (index in any.indices) {
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(any[index].toString())
                }
                sb.append("]")
                return sb.toString()
            }
            else -> return any.toString()
        }
    }

    private fun loadExplanations(stream: InputStream) {
        val namespaces = mutableMapOf<String,String>()
        var code = ""
        var variant = 1
        var message = ""
        var explanation = ""

        namespaces[""] = "http://xmlcalabash.com/ns/ERROR"

        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
        for (line in reader.readLines()) {
            if (line == "") {
                if (code != "") {
                    messages.add(ErrorExplanationTemplate(code, variant, message, explanation))
                    code = ""
                    variant = 1
                    message = ""
                    explanation = ""
                }
            } else {
                var match = Regex("^\\s*namespace\\s+(\\S+)\\s*=\\s*(.*)\\s*").find(line)
                if (match != null) {
                    val (prefix, uri) = match.destructured
                    namespaces[prefix] = uri
                } else {
                    var qcode: QName? = null
                    if (code == "") {
                        match = Regex("^([^{]\\S*):(\\S+)\\s*/\\s*(\\d+)\\s*$").find(line)
                        if (match != null) {
                            val (prefix, localName, vcode) = match.destructured
                            variant = vcode.toInt()
                            if (namespaces.containsKey(prefix)) {
                                qcode = QName(prefix, namespaces[prefix], localName)
                            } else {
                                // TODO: log error
                                qcode = QName("", namespaces[""], localName)
                            }
                        } else {
                            match = Regex("^([^{]\\S*):(\\S+)\\s*$").find(line)
                            if (match != null) {
                                val (prefix, localName) = match.destructured
                                variant = 1
                                if (namespaces.containsKey(prefix)) {
                                    qcode = QName(prefix, namespaces[prefix], localName)
                                } else {
                                    // TODO: log error
                                    qcode = QName("", namespaces[""], localName)
                                }
                            } else {
                                match = Regex("^([^:\\s]+)\\s*/\\s*(\\d+)\\s*$").find(line)
                                if (match != null) {
                                    val (bcode, vcode) = match.destructured
                                    variant = vcode.toInt()
                                    qcode = QName(NsErr.namespace, bcode)
                                } else {
                                    match = Regex("^([^:\\s]+)\\s*$").find(line)
                                    if (match != null) {
                                        variant = 1
                                        qcode = QName(NsErr.namespace, line)
                                    } else {
                                        match = Regex("^\\{(.*)}\\s*(\\S+)\\s*/\\s*(\\d+)\\s*$").find(line)
                                        if (match != null) {
                                            val (uri, localName, vcode) = match.destructured
                                            variant = vcode.toInt()
                                            qcode = QName(uri, localName)
                                        } else {
                                            match = Regex("^\\{(.*)}\\s*(\\S+)\\s*$").find(line)
                                            if (match != null) {
                                                val (uri, localName) = match.destructured
                                                variant = 1
                                                qcode = QName(uri, localName)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (qcode != null) {
                            code = qcode.clarkName
                        } else {
                            code = "????"
                        }
                    } else {
                        if (message == "") {
                            message = line
                        } else {
                            if (explanation == "") {
                                explanation = line
                            } else {
                                explanation += "\n${line}"
                            }
                        }
                    }
                }
           }
        }

        if (code != "") {
            messages.add(ErrorExplanationTemplate(code, variant, message, explanation))
        }
    }

    private fun showDetail(doc: XProcDocument): String {
        var message: String? = null
        if (doc is XProcBinaryDocument) {
            message = "...binary message cannot be displayed..."
        } else if (doc.contentType?.classification() == MediaClassification.TEXT) {
            message = doc.value.underlyingValue.stringValue
        } else if (doc.contentType?.classification() == MediaClassification.XML) {
            val root = if (S9Api.isTextDocument(doc.value as XdmNode)) {
                doc.value as XdmNode
            } else {
                S9Api.documentElement(doc.value as XdmNode)
            }

            if (root.nodeName in listOf(NsXvrl.reports, NsXvrl.report)) {
                return showXvrl(doc)
            }

            var markup = false
            for (node in root.axisIterator(Axis.CHILD)) {
                if (node.nodeKind != XdmNodeKind.TEXT) {
                    markup = true
                    break
                }
            }
            if (!markup) {
                message = root.underlyingValue.stringValue
            }
        }

        if (message == null) {
            val baos = ByteArrayOutputStream()
            val writer = DocumentWriter(doc, baos)
            writer.set(Ns.omitXmlDeclaration, "true")
            writer.set(Ns.indent, "true")
            writer.write()
            message = baos.toString(printer.encoding)
        }

        if (message == null) {
            message = "...no explanation provided..."
        }

        return message.trim()
    }

    private fun showXvrl(doc: XProcDocument): String {
        val sb = StringBuilder()
        val node = doc.value as XdmNode

        // This is a slightly odd context, we don't have access to a step configuration
        // so we can't call on the document manager to load the stylesheet. But we
        // can make sure we get the same one...
        val stylesheet = InternalDocumentResolver.URI_MAP[InternalDocumentResolver.XVRL_TO_TEXT]!!

        var styleStream = DefaultErrorExplanation::class.java.getResourceAsStream(stylesheet)
        var styleSource = SAXSource(InputSource(styleStream))
        styleSource.systemId = "/com/xmlcalabash/format-report.xsl"
        var xsltCompiler = node.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = node.processor.isSchemaAware
        var xsltExec = xsltCompiler.compile(styleSource)

        var transformer = xsltExec.load30()
        transformer.globalContextItem = node
        val xmlResult = XdmDestination()
        transformer.applyTemplates(node.asSource(), xmlResult)

        val baos = ByteArrayOutputStream()
        val writer = DocumentWriter(XProcDocument.ofText(xmlResult.xdmNode, doc.context), baos)
        writer.set(Ns.method, "text")
        writer.set(Ns.encoding, printer.encoding)
        writer.write()
        val text = baos.toString(StandardCharsets.UTF_8)
        return text
    }


    class ErrorExplanationTemplate(val code: String, val variant: Int, val message: String, val explanation: String) {
        val cardinality: Int = message.count { ch -> ch == '\$' }
        override fun toString(): String {
            return "${code}/${variant} (${cardinality}): ${message}"
        }
    }
}