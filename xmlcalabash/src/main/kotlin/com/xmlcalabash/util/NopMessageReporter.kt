package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.io.MessagePrinter
import net.sf.saxon.s9api.QName

open class NopMessageReporter(val nextReporter: MessageReporter? = null): MessageReporter {
    private var _messagePrinter: MessagePrinter? = null
    override var threshold = Verbosity.ERROR // irrelevant

    override val messagePrinter: MessagePrinter
        get() = _messagePrinter!!

    override fun setMessagePrinter(messagePrinter: MessagePrinter) {
        _messagePrinter = messagePrinter
    }

    override fun error(report: () -> Report) {
        report(Verbosity.ERROR, report)
    }

    override fun warn(report: () -> Report) {
        report(Verbosity.WARN, report)
    }

    override fun info(report: () -> Report) {
        report(Verbosity.INFO, report)
    }

    override fun debug(report: () -> Report) {
        report(Verbosity.DEBUG, report)
    }

    override fun trace(report: () -> Report) {
        report(Verbosity.TRACE, report)
    }

    override fun report(severity: Verbosity, report: () -> Report) {
        nextReporter?.report(severity, report)
    }
}