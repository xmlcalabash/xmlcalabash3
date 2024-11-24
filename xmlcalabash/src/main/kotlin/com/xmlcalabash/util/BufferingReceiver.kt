package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.api.Receiver

class BufferingReceiver(): Receiver {
    val outputs = mutableMapOf<String, MutableList<XProcDocument>>()

    override fun output(port: String, document: XProcDocument) {
        outputs.getOrPut(port) { mutableListOf() }.add(document)
    }

    internal fun close() {
        // nop
    }
}