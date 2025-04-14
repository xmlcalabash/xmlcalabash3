package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.graph.Model
import com.xmlcalabash.graph.PipelineModel
import com.xmlcalabash.graph.SubpipelineModel
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.CompoundStep
import net.sf.saxon.s9api.QName

class AtomicCompoundStepModel(runtime: XProcRuntime, model: SubpipelineModel): AtomicStepModel(runtime, model) {
    val impl = CompoundStepModel(runtime, model.model)
    private var _type = type
    val stepType: QName
        get() = _type

    override fun initialize(model: Model) {
        super.initialize(model)
        impl.initialize((model as SubpipelineModel).model)

        extensionAttributes.putAll(model.step.extensionAttributes)

        if (model.model is PipelineModel) {
            _type = (model.model.step as DeclareStepInstruction).type ?: NsCx.anonymous
        } else {
            _type = type
        }
    }

    override fun runnable(config: XProcStepConfiguration): () -> AbstractStep {
        return { CompoundStep.newInstance(config.copy(), impl) }
    }

    override fun toString(): String {
        return "${stepType}/${name}"
    }
}