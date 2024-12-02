package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.FootModel
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters

class CompoundStepFoot(yconfig: RuntimeStepConfiguration, step: FootModel): AbstractStep(yconfig, step) {
    internal var alwaysAllowSequences = false
    val cache = mutableMapOf<String, MutableList<XProcDocument>>()
    val holdPorts = mutableSetOf<String>()
    override val params = RuntimeStepParameters(NsCx.foot, "!foot",
        step.location, step.inputs, step.outputs, step.options)

    override val readyToRun: Boolean
        get() = true

    override fun input(port: String, doc: XProcDocument) {
        // N.B. inputs to a foot are outputs for the compound step
        checkOutputPort(port, doc, params.inputs[port])

        if (port in holdPorts) {
            val list = cache[port] ?: mutableListOf()
            list.add(doc)
            cache[port] = list
            return
        }

        write(port, doc)
    }

    override fun output(port: String, document: XProcDocument) {
        throw UnsupportedOperationException("Never send an output to a compound foot")
    }

    internal fun write(port: String, doc: XProcDocument) {
        val rpair = receiver[port]
        if (rpair == null) {
            println("No receiver for ${port} from ${this} (in foot)")
            return
        }

        val targetStep = rpair.first
        val targetPort = rpair.second
        targetStep.input(targetPort, doc)
    }

    override fun close(port: String) {
        // nop
    }

    override fun instantiate() {
        // nop
    }

    override fun run() {
        for ((port, flange) in params.inputs) {
            if (!flange.sequence && !alwaysAllowSequences && (outputCount[port]?:0) != 1) {
                throw XProcError.xdOutputSequenceForbidden(port).exception()
            }
        }

        cache.clear()
        for ((_, rpair) in receiver) {
            rpair.first.close(rpair.second)
        }
    }

    override fun reset() {
        super.reset()
        cache.clear()
    }

    override fun toString(): String {
        return "(compound step foot {$id})"
    }
}