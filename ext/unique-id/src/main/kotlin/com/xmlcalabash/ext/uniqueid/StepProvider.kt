package com.xmlcalabash.ext.uniqueid

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val ID_STEP_TYPE = QName(NsCx.namespace, "cx:unique-id")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return { -> UniqueIdStep() }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == ID_STEP_TYPE
    }

    override fun stepTypes(): Set<QName> {
        return setOf(ID_STEP_TYPE)
    }
}