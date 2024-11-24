package com.xmlcalabash.runtime

open class RuntimeOtherwiseStep(pipelineConfig: XProcRuntime): RuntimeWhenStep(pipelineConfig) {
    override fun evaluateTestExpression(): Boolean {
        return true
    }
}