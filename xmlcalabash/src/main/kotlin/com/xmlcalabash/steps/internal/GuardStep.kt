package com.xmlcalabash.steps.internal

import com.xmlcalabash.steps.AbstractAtomicStep

class GuardStep(): AbstractAtomicStep() {
    fun effectiveBooleanValue(): Boolean {
        val value = queues["source"]!!.first()
        return value.value.underlyingValue.effectiveBooleanValue()
    }

    override fun toString(): String = "cx:guard"
}