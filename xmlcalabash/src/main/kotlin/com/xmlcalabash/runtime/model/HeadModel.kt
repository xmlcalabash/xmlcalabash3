package com.xmlcalabash.runtime.model

import com.xmlcalabash.graph.Head
import com.xmlcalabash.graph.Model
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep

class HeadModel(runtime: XProcRuntime, model: Model): StepModel(runtime, model) {
    val defaultInputs = (model as Head).defaultInputs

    override fun initialize(model: Model) {
        // nop
    }

    override fun runnable(yconfig: RuntimeStepConfiguration): () -> AbstractStep {
        throw UnsupportedOperationException("You can't make a runnable from a head")
    }

    override fun toString(): String {
        return "(head)"
    }
}