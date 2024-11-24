package com.xmlcalabash.util

interface MessageReporter {
    var threshold: Verbosity
    fun warn(message: () -> String)
    fun info(message: () -> String)
    fun progress(message: () -> String)
    fun detail(message: () -> String)
}