package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.FootModel
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import java.time.Duration

class CompoundStepFoot(config: XProcStepConfiguration, val parent: CompoundStep, step: FootModel): AbstractStep(config, step, NsCx.foot, "${step.name}/foot") {
    internal var alwaysAllowSequences = false
    internal var looping = false
    val cache = mutableMapOf<String, MutableList<XProcDocument>>()
    val holdPorts = mutableSetOf<String>()
    override val params = RuntimeStepParameters(NsCx.foot, "!foot",
        step.location, step.inputs, step.outputs, step.options)

    override val stepTimeout: Duration = Duration.ZERO

    override val readyToRun: Boolean = true

    override fun input(port: String, doc: XProcDocument) {
        stepConfig.debug { "RECVD ${this} input on $port" }

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
        if (aborted) {
            return
        }

        val rpair = receiver[port]
        if (rpair == null) {
            stepConfig.debug { "No receiver for ${port} from ${this} (in foot)" }
            return
        }

        val targetStep = rpair.first
        val targetPort = rpair.second

        var outdoc = doc
        for (monitor in stepConfig.environment.monitors) {
            outdoc = monitor.sendDocument(Pair(this, port), Pair(targetStep, targetPort), outdoc)
        }

        targetStep.input(targetPort, outdoc)
    }

    override fun close(port: String) {
        stepConfig.debug { "CLOSE ${this} port ${port}" }
    }

    override fun instantiate() {
        // nop
    }

    override fun prepare() {
        // nop
    }

    override fun run() {
        for ((port, flange) in params.inputs) {
            if (!flange.sequence && !alwaysAllowSequences && (outputCount[port]?:0) != 1) {
                throw stepConfig.exception(XProcError.xdOutputSequenceForbidden(port))
            }
        }

        cache.clear()

        if (!looping) {
            for ((_, rpair) in receiver) {
                rpair.first.close(rpair.second)
            }
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