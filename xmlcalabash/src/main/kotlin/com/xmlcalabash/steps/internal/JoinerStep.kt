package com.xmlcalabash.steps.internal

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class JoinerStep(): AbstractAtomicStep() {
    val inputPorts = mutableListOf<String>()
    val outputPorts = mutableListOf<String>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        for (input in stepParams.inputs.keys) {
            inputPorts.add(input)
        }
        for (output in stepParams.outputs.keys) {
            outputPorts.add(output)
        }
    }

    override fun run() {
        super.run()
        for (iport in inputPorts) {
            for (oport in outputPorts) {
                for (doc in queues[iport] ?: emptyList()) {
                    receiver.output(oport, doc)
                }
            }
        }
    }

    override fun reset() {
        super.reset()
    }

    override fun toString(): String = "cx:joiner"
}