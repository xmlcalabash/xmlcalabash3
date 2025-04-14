package com.xmlcalabash.api

import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName

interface MessageReporter {
    val messagePrinter: MessagePrinter
    var threshold: Verbosity
    fun setMessagePrinter(messagePrinter: MessagePrinter)
    fun error(message: () -> String)
    fun warn(message: () -> String)
    fun info(message: () -> String)
    fun debug(message: () -> String)
    fun trace(message: () -> String)

    fun report(verbosity: Verbosity, extraAttributes: Map<QName,String> = emptyMap(), message: () -> String)
}