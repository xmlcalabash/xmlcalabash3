package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.graph.Model
import com.xmlcalabash.namespace.NsCx

class RuntimeFootStep(pipelineConfig: XProcRuntime): RuntimeAtomicStep(pipelineConfig) {
    override fun setup(model: Model) {
        _tag = NsCx.foot
        _stepConfig = model.step.stepConfig
        _inputManifold = model.inputs
        _outputManifold = model.outputs
        _optionManifold = model.options

        runtime.steps[model] = this
    }

    internal fun forwardOutputs(receiver: Receiver) {
        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                receiver.output(port, document)
            }
        }
        inputDocuments.clear()
    }

    internal fun forwardOutputs(pipelineConfig: XProcRuntime) {
        for ((port, documents) in inputDocuments) {
            pipelineConfig.output(port, documents)
        }
        inputDocuments.clear()
    }

    internal fun queue(port: String): List<XProcDocument> {
        return inputDocuments[port] ?: emptyList()
    }

    internal fun clear(port: String) {
        inputDocuments.remove(port)
    }

    override fun runStep() {
        // Leave the documents in the input list
    }

    override fun connectReceivers() {
        // Do nothing, they just pool up in here
    }

    override fun toString(): String {
        return "foot: ${tag}"
    }
}