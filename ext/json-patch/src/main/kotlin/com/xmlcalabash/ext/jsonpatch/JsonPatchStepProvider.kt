package com.xmlcalabash.ext.jsonpatch

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class JsonPatchStepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val cx_json_diff = QName(NsCx.namespace, "cx:json-diff")
        private val cx_json_patch = QName(NsCx.namespace, "cx:json-patch")
        private val STEPS = setOf(cx_json_diff, cx_json_patch)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun stepTypes(): Set<QName> {
        return STEPS
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return STEPS.contains(stepType)
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            cx_json_diff -> { -> JsonPatchDiffStep() }
            cx_json_patch -> { -> JsonPatchStep() }
            else -> throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
        }
    }
}
