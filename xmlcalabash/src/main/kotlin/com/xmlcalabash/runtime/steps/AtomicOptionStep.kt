package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.AtomicBuiltinStepModel
import net.sf.saxon.s9api.QName
import java.time.Duration

class AtomicOptionStep(config: XProcStepConfiguration, atomic: AtomicBuiltinStepModel, val externalName: QName): AtomicStep(config, atomic) {
    private var _externalValue: XProcDocument? = null
    var externalValue: XProcDocument?
        get() = _externalValue
        set(value) {
            _externalValue = value
        }
    internal val atomicOptionValues = mutableMapOf<QName, LazyValue>()

    override val stepTimeout: Duration = Duration.ZERO

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
            stepConfig.debug { "  Compute option value" }
            super.runImplementation()
        } else {
            stepConfig.debug { "  Option value: ${externalValue!!.value}" }
            output("result", externalValue!!)
        }
    }

}