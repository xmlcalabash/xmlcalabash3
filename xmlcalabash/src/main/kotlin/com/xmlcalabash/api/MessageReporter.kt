package com.xmlcalabash.api

import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName

interface MessageReporter {
    var threshold: Verbosity
    fun error(message: () -> String)
    fun warn(message: () -> String)
    fun info(message: () -> String)
    fun progress(message: () -> String)
    fun debug(message: () -> String)
    fun trace(message: () -> String)

    fun report(verbosity: Verbosity, extraAttributes: Map<QName,String> = emptyMap(), message: () -> String)
}