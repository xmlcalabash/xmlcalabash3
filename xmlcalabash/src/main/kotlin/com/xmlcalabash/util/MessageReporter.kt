package com.xmlcalabash.util

interface MessageReporter {
    var threshold: Verbosity
    fun error(message: () -> String)
    fun warn(message: () -> String)
    fun info(message: () -> String)
    fun progress(message: () -> String)
    fun debug(message: () -> String)
    fun trace(message: () -> String)
}