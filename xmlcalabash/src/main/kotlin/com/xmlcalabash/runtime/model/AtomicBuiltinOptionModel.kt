package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.graph.AtomicModel
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.AtomicOptionStep

class AtomicBuiltinOptionModel(runtime: XProcRuntime, model: AtomicModel): AtomicBuiltinStepModel(runtime, model) {
    override fun runnable(config: XProcStepConfiguration): () -> AbstractStep {
        val externalName = (model.step as AtomicExpressionStepInstruction).externalName!!
        return { AtomicOptionStep(config.copy(), this, externalName) }
    }
}