package com.xmlcalabash.runtime

import com.xmlcalabash.steps.internal.GuardStep

class RuntimeGuardStep(pipelineConfig: XProcRuntime): RuntimeAtomicStep(pipelineConfig) {
    fun effectiveBooleanValue(): Boolean {
        return (implementation as GuardStep).effectiveBooleanValue()
    }
}