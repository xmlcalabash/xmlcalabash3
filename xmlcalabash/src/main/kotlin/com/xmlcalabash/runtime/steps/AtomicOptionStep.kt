package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.AtomicBuiltinStepModel
import net.sf.saxon.s9api.QName

class AtomicOptionStep(yconfig: RuntimeStepConfiguration, atomic: AtomicBuiltinStepModel, val externalName: QName): AtomicStep(yconfig, atomic) {
    var externalValue: XProcDocument? = null
    internal val atomicOptionValues = mutableMapOf<QName, LazyValue>()

    override fun instantiate() {
        // nop
    }

    override fun run() {
        for ((name, value) in atomicOptionValues) {
            implementation.option(name, value)
        }
        super.run()
    }

    override fun runImplementation() {
        if (externalValue == null) {
            super.runImplementation()
        } else {
            output("result", externalValue!!)
        }
    }

}