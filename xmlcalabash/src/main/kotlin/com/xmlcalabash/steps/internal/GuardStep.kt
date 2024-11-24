package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.steps.AbstractAtomicStep

class GuardStep(): AbstractAtomicStep() {
    internal lateinit var value: XProcDocument

    override fun input(port: String, doc: XProcDocument) {
        value = doc
    }

    fun effectiveBooleanValue(): Boolean {
        return value.value.underlyingValue.effectiveBooleanValue()
    }

    override fun run() {
        //println("Running ${this}: ${effectiveBooleanValue()}")
    }

    override fun toString(): String = "cx:guard"
}