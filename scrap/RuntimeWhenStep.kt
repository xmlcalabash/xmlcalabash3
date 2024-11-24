package com.xmlcalabash.runtime

import com.xmlcalabash.steps.compound.WhenStep

open class RuntimeWhenStep(pipelineConfig: XProcRuntime): RuntimeSubpipelineStep(pipelineConfig) {
    internal open fun evaluateTestExpression(): Boolean {
        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                subpipeline.input(port, document)
            }
        }
        inputDocuments.clear()

        return (subpipeline as WhenStep).evaluateTestExpression()
    }
}