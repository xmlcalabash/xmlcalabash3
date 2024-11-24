package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.graph.Model
import com.xmlcalabash.namespace.NsCx

class RuntimeHeadStep(pipelineConfig: XProcRuntime): RuntimeAtomicStep(pipelineConfig) {
    override fun setup(model: Model) {
        _tag = NsCx.head
        _stepConfig = model.step.stepConfig
        _inputManifold = model.inputs
        _outputManifold = model.outputs
        _optionManifold = model.options

        runtime.steps[model] = this
    }

    internal fun input(port: String, document: XProcDocument) {
        if (!inputDocuments.containsKey(port)) {
            inputDocuments[port] = mutableListOf()
        }
        inputDocuments[port]!!.add(document)
    }

    override fun runStep() {
        for ((portName, port) in outputManifold) {
            val documents = inputDocuments[portName] ?: emptyList()
            if (documents.isNotEmpty()) {
                for (document in documents) {
                    receiver.output(portName, document)
                }
                inputDocuments.remove(portName)
                continue
            }
        }

        for ((port, documents) in inputDocuments) {
            if (!port.startsWith("Q{")) {
                for (document in documents) {
                    receiver.output(port, document)
                }
            }
        }

        receiver.close()
        inputDocuments.clear()
    }

    override fun toString(): String {
        return "head: ${tag}"
    }
}