package com.xmlcalabash.steps.internal

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class SplitterStep(): AbstractAtomicStep() {
    val outputPorts = mutableListOf<String>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        for (output in stepParams.outputs.keys) {
            outputPorts.add(output)
        }
    }

    override fun run() {
        super.run()
        for (doc in queues["source"]!!) {
            for (port in outputPorts) {
                receiver.output(port, doc)
            }
        }
    }

    override fun toString(): String = "cx:splitter"
}