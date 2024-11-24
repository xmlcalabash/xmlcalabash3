package com.xmlcalabash.runtime.model

import com.xmlcalabash.graph.Model
import com.xmlcalabash.runtime.XProcRuntime

abstract class AtomicStepModel(runtime: XProcRuntime, model: Model): StepModel(runtime, model) {
    override fun initialize(model: Model) {
        // nop
    }
}