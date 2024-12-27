package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import net.sf.saxon.s9api.QName

class DefaultMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        if (verbosity >= threshold) {
            println(message())
        }
        nextReporter?.report(verbosity, extraAttributes, message)
    }
}
