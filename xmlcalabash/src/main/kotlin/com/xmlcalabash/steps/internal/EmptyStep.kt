package com.xmlcalabash.steps.internal

import com.xmlcalabash.runtime.parameters.EmptyStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

class EmptyStep(val params: EmptyStepParameters): AbstractAtomicStep() {
    override fun run() {
        super.run()
        // nop
    }

    override fun toString(): String = "cx:empty"
}