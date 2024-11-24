package com.xmlcalabash.runtime.model

class ModelEdge(val fromStep: StepModel, val fromPort: String, val toStep: StepModel, val toPort: String) {
    override fun toString(): String {
        return "${fromStep}.${fromPort} → ${toStep}.${toPort}"
    }
}