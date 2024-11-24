package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.CatchInstruction
import com.xmlcalabash.datamodel.ViewportInstruction
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.graph.Model
import com.xmlcalabash.graph.SubpipelineModel
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.steps.compound.*

open class RuntimeSubpipelineStep(pipelineConfig: XProcRuntime): RuntimeStep(pipelineConfig) {
    private lateinit var _subpipeline: RuntimeCompoundStep
    val subpipeline: RuntimeCompoundStep
        get() {
            return _subpipeline
        }

    override fun setup(model: Model) {
        super.setup(model)

        val subpipelineModel = model as SubpipelineModel

        _subpipeline = when (tag) {
            NsP.group -> GroupStep(runtime)
            NsP.declareStep -> DeclareStepStep(runtime)
            NsP.forEach -> ForEachStep(runtime)
            NsP.choose -> ChooseStep(runtime)
            NsP.`when` -> WhenStep(runtime)
            NsP.otherwise -> OtherwiseStep(runtime)
            NsP.viewport -> ViewportStep(runtime, (model.step as ViewportInstruction).match)
            NsP.`try` -> TryStep(runtime)
            NsP.catch -> CatchStep(runtime, (model.step as CatchInstruction).code)
            NsP.finally -> FinallyStep(runtime)
            else -> throw XProcError.xsMissingStepDeclaration(tag).exception()
        }

        _subpipeline.setup(subpipelineModel.model)
    }

    override fun runStep() {
        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                subpipeline.input(port, document)
            }
        }
        inputDocuments.clear()

        subpipeline.run()
        subpipeline.foot.forwardOutputs(receiver)
    }

    override fun connectReceivers() {
        super.connectReceivers()
        subpipeline.connectReceivers()
    }
}