package com.xmlcalabash.ext.pipelinemessages

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val STEP = QName(NsCx.namespace, "cx:pipeline-messages")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        if (params.stepType == STEP) {
            return { -> PipelineMessagesStep() }
        }
        throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == STEP
    }

    override fun stepTypes(): Set<QName> {
        return setOf(STEP)
    }
}