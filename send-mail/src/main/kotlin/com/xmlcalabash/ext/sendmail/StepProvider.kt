package com.xmlcalabash.ext.sendmail

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val ID_STEP_TYPE = QName(NsP.namespace, "p:send-mail")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return { -> SendMail() }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == ID_STEP_TYPE
    }

    override fun stepTypes(): Set<QName> {
        return setOf(ID_STEP_TYPE)
    }
}