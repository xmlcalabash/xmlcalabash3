package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.io.MessagePrinter
import net.sf.saxon.s9api.QName

class DefaultMessageReporter(val printer: MessagePrinter, nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        if (verbosity >= threshold) {
            val prefix = when (verbosity) {
                Verbosity.TRACE -> "Trace: "
                Verbosity.DEBUG -> "Debug: "
                Verbosity.INFO -> ""
                Verbosity.WARN -> "Warning: "
                Verbosity.ERROR -> "Error: "
            }
            printer.println("${prefix}${message()}")
        }
        nextReporter?.report(verbosity, extraAttributes, message)
    }
}
