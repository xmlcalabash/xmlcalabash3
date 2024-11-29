package com.xmlcalabash.ext.cache

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class StepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val CACHE_ADD = QName(NsCx.namespace, "cx:cache-add")
        private val CACHE_DELETE = QName(NsCx.namespace, "cx:cache-delete")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        if (params.stepType == CACHE_ADD) {
            return { -> CacheAddStep() }
        } else if (params.stepType == CACHE_DELETE) {
            return { -> CacheDeleteStep() }
        }
        throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == CACHE_ADD || stepType == CACHE_DELETE
    }

    override fun stepTypes(): Set<QName> {
        return setOf(CACHE_ADD, CACHE_DELETE)
    }
}