package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import net.sf.saxon.s9api.QName

open class NopMessageReporter(val nextReporter: MessageReporter? = null): MessageReporter {
    override var threshold = Verbosity.ERROR // irrelevant

    override fun error(message: () -> String) {
        report(Verbosity.ERROR, emptyMap(), message)
    }

    override fun warn(message: () -> String) {
        report(Verbosity.WARN, emptyMap(), message)
    }

    override fun info(message: () -> String) {
        report(Verbosity.INFO, emptyMap(), message)
    }

    override fun progress(message: () -> String) {
        report(Verbosity.PROGRESS, emptyMap(), message)
    }

    override fun debug(message: () -> String) {
        report(Verbosity.DEBUG, emptyMap(), message)
    }

    override fun trace(message: () -> String) {
        report(Verbosity.TRACE, emptyMap(), message)
    }

    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        nextReporter?.report(verbosity, extraAttributes, message)
    }
}
