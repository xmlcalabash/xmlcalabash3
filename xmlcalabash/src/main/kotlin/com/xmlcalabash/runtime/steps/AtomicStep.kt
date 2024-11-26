package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.model.AtomicBuiltinStepModel
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.XdmValue

open class AtomicStep(yconfig: RuntimeStepConfiguration, atomic: AtomicBuiltinStepModel): AbstractStep(yconfig, atomic), Receiver {
    final override val params = RuntimeStepParameters(atomic.type, atomic.name, atomic.location,
        atomic.inputs, atomic.outputs, atomic.options)
    val implementation = atomic.provider()
    val inputPorts = mutableSetOf<String>()
    val initiallyOpenPorts = mutableSetOf<String>()
    val openPorts = mutableSetOf<String>()
    private var message: XdmValue? = null
    private val rteContext = yconfig.rteContext
    private val inputErrors = mutableListOf<XProcError>()

    init {
        inputPorts.addAll(atomic.inputs.keys)
        for ((name, port) in atomic.inputs) {
            if (!port.weldedShut) {
                initiallyOpenPorts.add(name)
            }
        }
        openPorts.addAll(initiallyOpenPorts)

        implementation.setup(stepConfig, this, params)
    }

    override val readyToRun: Boolean
        get() {
            for (portName in openPorts) {
                val port = params.inputs[portName]!!
                if (!port.unbound) {
                    return false
                }
            }
            return true
        }

    override fun input(port: String, doc: XProcDocument) {
        val error = checkInputPort(port, doc, params.inputs[port])
        if (error == null) {
            if (port.startsWith("Q{")) {
                val name = stepConfig.parseQName(port)
                if ((type.namespaceUri == NsP.namespace && name == Ns.message)
                    || (type.namespaceUri != NsP.namespace && name == NsP.message)) {
                    message = doc.value
                } else {
                    implementation.option(name, LazyValue(doc.context, doc.value))
                }
                return
            }
            implementation.input(port, doc)
        } else {
            inputErrors.add(error)
        }
    }

    override fun output(port: String, document: XProcDocument) {
        checkOutputPort(port, document, params.outputs[port])

        val rpair = receiver[port]
        if (rpair == null) {
            println("No receiver for ${port} from ${this} (in step)")
            return
        }

        val targetStep = rpair.first
        val targetPort = rpair.second
        targetStep.input(targetPort, document)
    }

    override fun close(port: String) {
        openPorts.remove(port)
    }

    override fun instantiate() {
        // nop
    }

    override fun run() {
        for ((name, details) in staticOptions) {
            if ((type.namespaceUri == NsP.namespace && name == Ns.message)
                || (type.namespaceUri != NsP.namespace && name == NsP.message)) {
                message = details.staticValue.evaluate()
            } else {
                implementation.option(name, LazyValue(details.stepConfig, details.staticValue))
            }
        }

        if (message != null) {
            println(message)
            message = null
        }

        if (inputErrors.isNotEmpty()) {
            throw inputErrors.first().exception()
        }

        for (portName in openPorts) {
            val port = params.inputs[portName]!!
            if (!port.weldedShut && port.defaultBindings.isEmpty()) {
                throw XProcError.xiImpossible("Unbound input port with no default bindings?").exception()
            }
            for (binding in port.defaultBindings) {
                for (document in defaultBindingDocuments(binding)) {
                    input(portName, document)
                }
            }
        }

        // If no input was sent to a port, it will not have been checked.
        for (portName in inputPorts) {
            if ((inputCount[portName] ?: 0) == 0) {
                val flange = params.inputs[portName]
                if (flange != null && !flange.sequence) {
                    throw XProcError.xdInputSequenceForbidden(portName).exception()
                }
            }
        }

        val stepConfig = (implementation as AbstractAtomicStep).stepConfig
        rteContext.newExecutionContext(stepConfig)
        runImplementation()
        rteContext.releaseExecutionContext()

        for ((_, rpair) in receiver) {
            rpair.first.close(rpair.second)
        }
    }

    internal open fun runImplementation() {
        implementation.run()
    }

    override fun reset() {
        super.reset()
        implementation.reset()
        openPorts.clear()
        openPorts.addAll(initiallyOpenPorts)
        inputErrors.clear()
    }
}