package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.graph.Foot
import com.xmlcalabash.graph.Model
import com.xmlcalabash.graph.ModelOption
import com.xmlcalabash.graph.ModelPort
import com.xmlcalabash.datamodel.StepConfiguration
import net.sf.saxon.s9api.QName

abstract class RuntimeStep(var runtime: XProcRuntime) {
    protected lateinit var _tag: QName
    protected lateinit var _name: String
    protected lateinit var _stepConfig: StepConfiguration
    protected lateinit var _inputManifold: Map<String, ModelPort>
    protected lateinit var _outputManifold: Map<String, ModelPort>
    protected lateinit var _optionManifold: Map<QName, ModelOption>

    val tag: QName
        get() {
            return _tag
        }
    val name: String
        get() {
            return _name
        }
    val stepConfig: StepConfiguration
        get() {
            return _stepConfig
        }
    val inputManifold: Map<String, ModelPort>
        get() {
            return _inputManifold
        }
    val outputManifold: Map<String, ModelPort>
        get() {
            return _outputManifold
        }
    val optionManifold: Map<QName, ModelOption>
        get() {
            return _optionManifold
        }

    private val inputOpen = mutableMapOf<String, Boolean>()
    protected val inputDocuments = mutableMapOf<String, MutableList<XProcDocument>>()
    val receiver = StepReceiver()

    internal val readyToRun: Boolean
        get() {
            for ((_, open) in inputOpen) {
                if (open) {
                    return false
                }
            }
            return true
        }

    lateinit private var _model: Model
    internal open fun cfg(model: Model) {
        _model = model
    }

    internal open fun insta() {
        setup(_model)
    }

    internal open fun setup(model: Model) {
        _tag = model.step.instructionType
        _name = model.step.name
        _stepConfig = model.step.stepConfig
        _inputManifold = model.inputs
        _outputManifold = model.outputs
        _optionManifold = model.options

        runtime.steps[model] = this

        for ((port, _) in inputManifold) {
            inputOpen[port] = true
        }
    }

    fun run() {
        //println("Running ${this}")
        try {
            runStep()
        } catch (ex: Exception) {
            if (this is RuntimeSubpipelineStep) {
                // We don't have to wrap exceptions here, or add to the stack
                // because all subpipelines are run by steps and we will do it
                // in the step.
                throw ex;
            }
            when (ex) {
                is XProcException -> {
                    ex.error.at(tag, name).at(stepConfig.location)
                    throw ex
                }
                else -> {
                    val err = XProcError.xdStepFailed(ex.message ?: "").at(tag, name)
                    throw err.exception(ex)
                }
            }
        }
    }

    internal abstract fun runStep()

    internal fun receive(port: String, document: XProcDocument) {
        if (port !in inputDocuments) {
            inputDocuments[port] = mutableListOf()
        }
        inputDocuments[port]!!.add(document)
    }

    internal fun close(port: String) {
        inputOpen[port] = false
    }

    internal open fun reset() {
        for ((port, _) in inputOpen) {
            inputOpen[port] = true
            inputDocuments[port]?.clear()
        }
    }

    internal open fun connectReceivers() {
        /*
        for ((name, port) in outputManifold) {
            if (port.parent is Foot) {
                continue
            }

            val conn = runtime.graph!!.connections.filter { it.from == port }.firstOrNull()
            if (conn == null) {
                // No one reads from this port...
                receiver.addDispatcher(name, null as RuntimeStep?, "!unconnected") // throw it away
            } else {
                val inputStep = runtime.steps[conn.to.parent]
                receiver.addDispatcher(name, inputStep, conn.to.name)
            }
        }

         */
    }

    override fun toString(): String {
        return "${tag}"
    }

}
