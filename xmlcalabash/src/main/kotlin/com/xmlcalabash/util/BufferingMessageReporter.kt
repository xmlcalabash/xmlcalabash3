package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import net.sf.saxon.s9api.QName
import java.time.LocalDateTime

class BufferingMessageReporter(val maxsize: Int, nextReporter: MessageReporter): NopMessageReporter(nextReporter) {
    constructor(nextReporter: MessageReporter): this(32, nextReporter)

    private val _messages = mutableListOf<Message>()

    fun messages(threshold: Verbosity): List<Message> {
        return _messages.filter { it.level >= threshold }
    }

    fun clear() {
        synchronized(_messages) {
            _messages.clear()
        }
    }

    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        addMessage(verbosity, extraAttributes, message)
        nextReporter?.report(verbosity, extraAttributes, message)
    }

    private fun addMessage(level: Verbosity, extra: Map<QName, String>, message: () -> String) {
        synchronized(_messages) {
            _messages.add(Message(level, extra, message))
            if (maxsize >= 0 && _messages.size > maxsize) {
                _messages.removeAt(0)
            }
        }
    }

    class Message(val level: Verbosity, extra: Map<QName, String>, val message: () -> String) {
        val attributes = mutableMapOf<QName, String>()
        val timestamp = LocalDateTime.now()
        init {
            attributes.putAll(extra)
        }
    }
}