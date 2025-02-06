package com.xmlcalabash.util

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

open class SimpleStepProvider(val step: QName, val implementation: () -> XProcStep): AtomicStepProvider, AtomicStepManager {
    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        if (params.stepType == step) {
            return implementation
        }
        throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == step
    }

    override fun stepTypes(): Set<QName> {
        return setOf(step)
    }
}