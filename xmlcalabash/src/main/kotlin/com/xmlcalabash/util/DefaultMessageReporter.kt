package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName

class DefaultMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        if (verbosity >= threshold) {
            val prefix = StringBuilder()

            when (verbosity) {
                Verbosity.TRACE -> prefix.append("Trace")
                Verbosity.DEBUG -> prefix.append("Debug")
                Verbosity.INFO -> Unit
                Verbosity.WARN -> prefix.append("Warning")
                Verbosity.ERROR -> prefix.append("Error")
            }

            if (verbosity == Verbosity.WARN || verbosity == Verbosity.ERROR) {
                val uri = extraAttributes[Ns.baseUri] ?: extraAttributes[Ns.systemIdentifier]
                if (uri != null) {
                    prefix.append(" at ").append(uri).append(":")
                    extraAttributes[Ns.lineNumber]?.let { prefix.append(it).append(":") }
                    extraAttributes[Ns.columnNumber]?.let { prefix.append(it).append(":") }
                    prefix.append(" ")
                }
            } else if (verbosity != Verbosity.INFO) {
                prefix.append(": ")
            }

            try {
                messagePrinter.println("${prefix}${message()}")
            } catch (ex: Exception) {
                messagePrinter.println("${prefix} failed to evaluate message: ${ex.message}")
            }
        }
        nextReporter?.report(verbosity, extraAttributes, message)
    }
}
