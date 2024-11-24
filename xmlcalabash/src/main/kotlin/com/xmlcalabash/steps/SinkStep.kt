package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument

open class SinkStep(): AbstractAtomicStep() {
    override fun input(port: String, doc: XProcDocument) {
        // ignore them
    }

    override fun toString(): String = "p:sink"
}