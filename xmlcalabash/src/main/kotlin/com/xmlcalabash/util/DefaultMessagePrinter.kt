package com.xmlcalabash.util

import com.xmlcalabash.io.MessagePrinter
import java.io.PrintStream

class DefaultMessagePrinter(override val encoding: String) : MessagePrinter {
    private var _printStream: PrintStream = System.out
    private val stream: PrintStream
        get() = _printStream
    private val mustSanitize = !encoding.lowercase().startsWith("utf")

    override fun setPrintStream(stream: PrintStream) {
        _printStream = stream
    }

    override fun print(message: String) {
        if (mustSanitize) {
            stream.print(sanitize(message))
        } else {
            stream.print(message)
        }
    }

    override fun println(message: String) {
        if (mustSanitize) {
            stream.println(sanitize(message))
        } else {
            stream.println(message)
        }
    }

    private fun sanitize(message: String): String {
        return message.replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\'')
            .replace("…".toRegex(), "...")
    }
}