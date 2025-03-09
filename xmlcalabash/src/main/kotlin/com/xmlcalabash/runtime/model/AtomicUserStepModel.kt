package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.StaticOptionDetails
import com.xmlcalabash.graph.AtomicModel
import com.xmlcalabash.graph.Model
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.CompoundStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode

class AtomicUserStepModel(runtime: XProcRuntime, model: AtomicModel, private val impl: CompoundStepModel): AtomicStepModel(runtime, model) {
    private var _type = type
    val stepType: QName
        get() = _type

    override fun initialize(model: Model) {
        super.initialize(model)

        impl.userStep = this
        extensionAttributes.putAll(model.step.extensionAttributes)

        for ((name, moption) in model.options) {
            if (moption.staticValue != null) {
                val details = StaticOptionDetails(model.step.stepConfig, name, moption.asType, moption.values, moption.staticValue!!)
                staticOptions[name] = details
            }
        }
    }

    override fun runnable(config: XProcStepConfiguration): () -> AbstractStep {
        // In this one case, the compound step's model inputs aren't the
        // important ones, *this* step's model inputs are the important ones.
        // What a lot of hackery.
        val step = synchronized(impl) {
            val saveCompoundInputs = mutableMapOf<String, RuntimePort>()
            saveCompoundInputs.putAll(impl.inputs)

            val saveHeadOutputs = mutableMapOf<String, RuntimePort>()
            saveHeadOutputs.putAll(impl.head.outputs)

            impl._inputs.clear()
            impl._inputs.putAll(inputs)

            val headPorts = impl.head.outputs.keys.toList()
            impl.head._outputs.clear()
            for (name in headPorts) {
                impl.head._outputs[name] = impl.inputs[name]!!
            }

            val instance = CompoundStep.newInstance(config, impl)
            instance.stepType = stepType
            instance.stepName = name

            // Validation mode on an instance overrides the default...
            if (stepConfig.validationMode != ValidationMode.DEFAULT) {
                instance.head.stepConfig.validationMode = stepConfig.validationMode
            }

            impl._inputs.clear()
            impl._inputs.putAll(saveCompoundInputs)

            impl.head._outputs.clear()
            impl.head._outputs.putAll(saveHeadOutputs)

            instance
        }

        for ((port, flange) in inputs) {
            if (flange.unbound) {
                step.head.unboundInputs.add(port)
            }
        }

        step.staticOptions.putAll(staticOptions)
        step.head.staticOptions.putAll(staticOptions)

        return { step }
    }

    override fun toString(): String {
        return "${stepType}/${name}"
    }
}