package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class JoinerStep(): AbstractAtomicStep() {
    val inputPorts = mutableListOf<String>()
    val queues = mutableMapOf<String,MutableList<XProcDocument>>()
    val outputPorts = mutableListOf<String>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        for (input in stepParams.inputs.keys) {
            inputPorts.add(input)
            queues.put(input, mutableListOf())
        }
        for (output in stepParams.outputs.keys) {
            outputPorts.add(output)
        }
    }

    override fun input(port: String, doc: XProcDocument) {
        queues[port]?.add(doc)
    }

    override fun run() {
        super.run()
        for (iport in inputPorts) {
            for (oport in outputPorts) {
                while (queues[iport]!!.isNotEmpty()) {
                    receiver.output(oport, queues[iport]!!.removeFirst())
                }
            }
        }
    }

    override fun reset() {
        super.reset()
        for ((_, list) in queues) {
            list.clear()
        }
    }

    override fun toString(): String = "cx:joiner"
}