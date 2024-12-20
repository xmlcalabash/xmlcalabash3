package com.xmlcalabash.exceptions

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.namespace.NsErr
import net.sf.saxon.s9api.QName
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class DefaultErrorExplanation(): ErrorExplanation {
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

    override fun message(error: XProcError) {
        val message = template(error.code, error.variant, error.details.size).message
        System.err.println("Fatal ${error.code}: ${substitute(message, *error.details)}")
        if (error.location != Location.NULL) {
            System.err.println("   at ${error.location}")
        }
        if (error.inputLocation != Location.NULL) {
            System.err.println("   in ${error.inputLocation}")
        }
        if (error.throwable != null && error.throwable?.message != null) {
            System.err.println("   cause: ${error.throwable!!.toString()}")
        }
    }

    override fun explanation(error: XProcError) {
        val message = template(error.code, error.variant, error.details.size).explanation
        System.err.println(substitute(message, *error.details))
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
                if (code == "") {
                    var qcode: QName? = null
                    var match = Regex("^\\s*namespace\\s+(\\S+)\\s*=\\s*(.*)\\s*").find(line)
                    if (match != null) {
                        val (prefix, uri) = match.destructured
                        namespaces[prefix] = uri
                    } else {
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

        if (code != "") {
            messages.add(ErrorExplanationTemplate(code, variant, message, explanation))
        }
    }

    class ErrorExplanationTemplate(val code: String, val variant: Int, val message: String, val explanation: String) {
        val cardinality: Int = message.count { ch -> ch == '\$' }
        override fun toString(): String {
            return "${code}/${variant} (${cardinality}): ${message}"
        }
    }
}