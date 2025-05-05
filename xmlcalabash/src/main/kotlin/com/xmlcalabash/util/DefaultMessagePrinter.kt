package com.xmlcalabash.util

import com.xmlcalabash.io.MessagePrinter
import org.apache.logging.log4j.kotlin.logger
import java.io.PrintStream

class DefaultMessagePrinter(encoding: String) : MessagePrinter {
    private var _encoding: String = encoding
    private var _printStream: PrintStream = System.err
    private val stream: PrintStream
        get() = _printStream
    private val mustSanitize = !encoding.lowercase().startsWith("utf")

    override val encoding: String = _encoding

    override fun setEncoding(encoding: String) {
        _encoding = encoding
    }

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
        return message.replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace("→", "->")
            .replace("…".toRegex(), "...")
    }
}