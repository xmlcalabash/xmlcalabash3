package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.api.Receiver

class DiscardingReceiver(): Receiver {
    override fun output(port: String, document: XProcDocument) {
        // Circular file, incoming!
    }

    internal fun close() {
        // nop
    }
}