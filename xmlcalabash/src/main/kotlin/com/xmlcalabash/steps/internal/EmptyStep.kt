package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.parameters.EmptyStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

class EmptyStep(val params: EmptyStepParameters): AbstractAtomicStep() {
    override fun input(port: String, doc: XProcDocument) {
        // like that's going to happen!
    }

    override fun run() {
        super.run()
        // nop
    }

    override fun toString(): String = "cx:empty"
}