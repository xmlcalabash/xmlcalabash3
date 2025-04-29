package com.xmlcalabash.api

import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName

interface MessageReporter {
    val messagePrinter: MessagePrinter
    var threshold: Verbosity
    fun setMessagePrinter(messagePrinter: MessagePrinter)
    fun error(report: () -> Report)
    fun warn(report: () -> Report)
    fun info(report: () -> Report)
    fun debug(report: () -> Report)
    fun trace(report: () -> Report)

    fun report(severity: Verbosity, report: () -> Report)
}