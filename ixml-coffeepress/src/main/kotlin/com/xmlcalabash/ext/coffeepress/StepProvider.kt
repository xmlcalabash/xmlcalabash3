package com.xmlcalabash.ext.coffeepress

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val ID_STEP_TYPES = setOf(
            QName(NsP.namespace, "p:invisible-xml"),
            QName(NsP.namespace, "p:ixml"),
            QName(NsCx.namespace, "cx:invisible-xml"))
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return { -> CoffeePress() }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return ID_STEP_TYPES.contains(stepType)
    }

    override fun stepTypes(): Set<QName> {
        return ID_STEP_TYPES
    }
}
