package com.xmlcalabash.ext.polyglot

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val POLYGLOT = QName(NsCx.namespace, "polyglot")
        private val JAVASCRIPT = QName(NsCx.namespace, "cx:javascript")
        private val PYTHON = QName(NsCx.namespace, "cx:python")
        private val RUBY = QName(NsCx.namespace, "cx:ruby")
        private val R = QName(NsCx.namespace, "cx:r")
        private val JAVA = QName(NsCx.namespace, "cx:java")
        private val STEP_TYPES = setOf(POLYGLOT, JAVASCRIPT, PYTHON, RUBY, R, JAVA)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            POLYGLOT -> { -> PolyglotStep("dynamic") }
            JAVASCRIPT -> { -> PolyglotStep("js") }
            PYTHON -> { -> PolyglotStep("python") }
            RUBY -> { -> PolyglotStep("ruby") }
            R -> { -> PolyglotStep("r") }
            JAVA -> { -> PolyglotStep("java") }
            else -> throw XProcError.xiImpossible("Unexpected step type: ${params.stepType}").exception()
        }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return STEP_TYPES.contains(stepType)
    }

    override fun stepTypes(): Set<QName> {
        return STEP_TYPES
    }
}