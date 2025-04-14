package com.xmlcalabash.io

import java.io.PrintStream

interface MessagePrinter {
    val encoding: String
    fun setEncoding(encoding: String)
    fun setPrintStream(stream: PrintStream)
    fun print(message: String)
    fun println(message: String)
}