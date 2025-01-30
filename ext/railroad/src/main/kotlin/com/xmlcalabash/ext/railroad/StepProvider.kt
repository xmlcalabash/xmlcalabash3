package com.xmlcalabash.ext.rr

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val cx_railroad = QName(NsCx.namespace, "cx:railroad")
        private val STEPS = setOf(cx_railroad)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            cx_railroad -> { -> RailroadStep() }
            else -> {
                throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
            }
        }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType in STEPS
    }

    override fun stepTypes(): Set<QName> {
        return STEPS
    }
}