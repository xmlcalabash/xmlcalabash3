package com.xmlcalabash.ext.diagramming

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val cx_ditaa = QName(NsCx.namespace, "cx:ditaa")
        private val cx_plantuml = QName(NsCx.namespace, "cx:plantuml")
        private val cx_mathmlToSvg = QName(NsCx.namespace, "cx:mathml-to-svg")

        private val STEPS = setOf(cx_ditaa, cx_plantuml, cx_mathmlToSvg)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            cx_ditaa -> { -> DitaaStep() }
            cx_plantuml -> { -> PlantumlStep() }
            cx_mathmlToSvg -> { -> MathMLtoSvgStep() }
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