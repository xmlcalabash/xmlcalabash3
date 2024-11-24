package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class SplitterStep(): AbstractAtomicStep() {
    val outputPorts = mutableListOf<String>()
    val queue = mutableListOf<XProcDocument>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        for (output in stepParams.outputs.keys) {
            outputPorts.add(output)
        }
    }

    override fun input(port: String, doc: XProcDocument) {
        queue.add(doc)
    }

    override fun run() {
        super.run()
        while (queue.isNotEmpty()) {
            val doc = queue.removeFirst()
            for (port in outputPorts) {
                receiver.output(port, doc)
            }
        }
    }

    override fun reset() {
        super.reset()
        queue.clear()
    }

    override fun toString(): String = "cx:splitter"
}