package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.graph.AtomicModel
import com.xmlcalabash.graph.CompoundModel
import com.xmlcalabash.graph.Model
import com.xmlcalabash.graph.SubpipelineModel
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP

open class RuntimeCompoundStep(runtime: XProcRuntime): RuntimeStep(runtime) {
    val steps = mutableListOf<RuntimeStep>()
    private lateinit var _head: RuntimeHeadStep
    private lateinit var _foot: RuntimeFootStep

    internal val head: RuntimeHeadStep
        get() = _head

    internal val foot: RuntimeFootStep
        get() = _foot

    override fun setup(model: Model) {
        super.setup(model)

        val compoundModel = model as CompoundModel
        _head = RuntimeHeadStep(runtime)
        _head.setup(compoundModel.head)
        _foot = RuntimeFootStep(runtime)
        _foot.setup(compoundModel.foot)

        for (childModel in compoundModel.children) {
            val step = when (childModel) {
                is SubpipelineModel -> {
                    when (childModel.step.instructionType) {
                        NsP.`when` -> RuntimeWhenStep(runtime)
                        NsP.otherwise -> RuntimeOtherwiseStep(runtime)
                        else -> RuntimeSubpipelineStep(runtime)
                    }
                }
                is AtomicModel -> {
                    if (childModel.step.instructionType == NsCx.guard) {
                        RuntimeGuardStep(runtime)
                    } else {
                        if (childModel.step is AtomicExpressionStepInstruction && childModel.step.externalName != null) {
                            val optstep = RuntimeOptionStep(runtime)
                            optstep.optionName = childModel.step.externalName!!
                            optstep
                        } else {
                            RuntimeAtomicStep(runtime)
                        }
                    }
                }
                else -> TODO("No support for ${childModel}")
            }
            step.setup(childModel)
            steps.add(step)
        }
    }

    internal fun input(port: String, document: XProcDocument) {
        if (inputDocuments.contains(port)) {
            inputDocuments[port]!!.add(document)
        } else {
            inputDocuments[port] = mutableListOf(document)
        }
    }

    override fun runStep() {
        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                head.input(port, document)
            }
        }
        inputDocuments.clear()

        head.run()

        val remaining = runTheseStepsExhaustively(steps)
        if (remaining.isNotEmpty()) {
            throw XProcError.xiNoRunnableSteps().exception()
        }
    }

    protected fun runTheseStepsExhaustively(steps: List<RuntimeStep>): List<RuntimeStep> {
        val stepsToRun = mutableListOf<RuntimeStep>()
        stepsToRun.addAll(steps)

        while (stepsToRun.isNotEmpty()) {
            var runMe: RuntimeStep? = null
            for (step in stepsToRun) {
                if (step.readyToRun) {
                    runMe = step
                    break
                }
            }
            if (runMe == null) {
                return stepsToRun
            }
            stepsToRun.remove(runMe)
            runMe.run()
            runMe.receiver.close()
        }

        return emptyList()
    }

    override fun connectReceivers() {
        super.connectReceivers()
        head.connectReceivers()
        foot.connectReceivers()
        for (step in steps) {
            step.connectReceivers()
        }
    }
}