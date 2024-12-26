package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.AtomicBuiltinStepModel
import net.sf.saxon.s9api.QName

class AtomicOptionStep(config: XProcStepConfiguration, atomic: AtomicBuiltinStepModel, val externalName: QName): AtomicStep(config, atomic) {
    var externalValue: XProcDocument? = null
    internal val atomicOptionValues = mutableMapOf<QName, LazyValue>()

    override val stepTimeout: Long = 0

    override fun instantiate() {
        // nop
    }

    override fun prepare() {
        for ((name, value) in atomicOptionValues) {
            implementation.option(name, value)
        }
    }

    override fun run() {
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