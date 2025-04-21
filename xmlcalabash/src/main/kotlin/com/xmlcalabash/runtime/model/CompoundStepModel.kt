package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.datamodel.CatchInstruction
import com.xmlcalabash.graph.*
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.parameters.RunStepStepParameters
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.runtime.parameters.TryCatchStepParameters
import com.xmlcalabash.runtime.parameters.ViewportStepParameters
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.CompoundStep

class CompoundStepModel(runtime: XProcRuntime, model: CompoundModel): StepModel(runtime, model) {
    internal var userStep: AtomicUserStepModel? = null
    lateinit var head: HeadModel
    lateinit var foot: FootModel
    lateinit var params: RuntimeStepParameters
    val steps = mutableListOf<StepModel>()
    val edges = mutableListOf<ModelEdge>()
    val timeout = model.timeout
    val childThreadGroups = model.childThreadGroups

    init {
        staticOptions.putAll(model.step.staticOptions)
        extensionAttributes.putAll(model.step.extensionAttributes)
    }

    override fun initialize(model: Model) {
        val cmodel = model as CompoundModel

        when (cmodel.step.instructionType) {
            NsP.forEach -> {
                val finputs = mutableMapOf<String, RuntimePort>()
                finputs.putAll(inputs)
                //finputs.remove("!source")
                params = RuntimeStepParameters(type, name, location, finputs, outputs, options)
            }
            NsP.viewport -> {
                val finputs = mutableMapOf<String, RuntimePort>()
                finputs.putAll(inputs)
                //finputs.remove("!source")
                params = ViewportStepParameters(type, name, location, finputs, outputs, options)
            }
            NsP.catch -> {
                params = TryCatchStepParameters(type, name, location, inputs, outputs, options,
                    (cmodel.step as CatchInstruction).code)
            }
            NsP.finally -> {
                params = TryCatchStepParameters(type, name, location, inputs, outputs, options, emptyList())
            }
            NsP.run -> {
                var primaryInput: String? = null
                var primaryOutput: String? = null
                for ((name, port) in cmodel.inputs) {
                    if (port.primary == true) {
                        primaryInput = name
                        break
                    }
                }
                for ((name, port) in cmodel.outputs) {
                    if (port.primary == true) {
                        primaryOutput = name
                        break
                    }
                }

                params = RunStepStepParameters(type, name, location, inputs, outputs, options, primaryInput, primaryOutput)
            }
            else -> {
                val pinputs = mutableMapOf<String, RuntimePort>()
                pinputs.putAll(inputs)
                if (userStep != null) {
                    for ((name, port) in userStep!!.inputs) {
                        if (name !in pinputs && !port.weldedShut) {
                            pinputs[name] = RuntimePort(port)
                        }
                    }
                }
                params = RuntimeStepParameters(type, name, location, pinputs, outputs, options)
            }
        }

        extensionAttributes.putAll(cmodel.step.extensionAttributes)

        val childModels = mutableMapOf<Model, StepModel>()
        head = HeadModel(runtime, cmodel.head)
        head.initialize(cmodel.head)

        childModels[cmodel.head] = head

        for (child in cmodel.children) {
            val step = when (child) {
                is AtomicModel -> {
                    val userStep = runtime.runnables[child.step.declId]
                    if (userStep == null) {
                        val atomic = if (child.step is AtomicExpressionStepInstruction && child.step.externalName != null) {
                            AtomicBuiltinOptionModel(runtime, child)
                        } else {
                            AtomicBuiltinStepModel(runtime, child)
                        }
                        atomic.initialize(child)
                        atomic
                    } else {
                        val atomic = AtomicUserStepModel(runtime, child, userStep)
                        atomic.initialize(child)
                        atomic
                    }
                }
                is SubpipelineModel -> {
                    val user = AtomicCompoundStepModel(runtime, child)
                    user.initialize(child)
                    user
                }
                else -> TODO("NOT IMPL")
            }
            childModels[child] = step
            steps.add(step)
        }

        foot = FootModel(runtime, cmodel.foot)
        foot.initialize(cmodel.foot)

        childModels[cmodel.foot] = foot

        for (edge in cmodel.graph.edges) {
            if (childModels.contains(edge.from) || childModels.contains(edge.to)) {
                val yedge = ModelEdge(childModels[edge.from]!!, edge.outputPort, childModels[edge.to]!!, edge.inputPort)
                edges.add(yedge)
            }
        }
    }

    override fun runnable(config: XProcStepConfiguration): () -> AbstractStep {
        return { CompoundStep.newInstance(config, this) }
    }
}