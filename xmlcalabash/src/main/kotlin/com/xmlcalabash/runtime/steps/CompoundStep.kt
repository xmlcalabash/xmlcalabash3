package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.model.StepModel
import com.xmlcalabash.steps.internal.ExpressionStep
import net.sf.saxon.s9api.QName

abstract class CompoundStep(config: XProcStepConfiguration, compound: CompoundStepModel): AbstractStep(config, compound) {
    companion object {
        fun newInstance(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep {
            val newConfig = config.copy(compound.stepConfig)
            when (compound.type) {
                NsP.declareStep -> return PipelineStep(newConfig, compound)
                NsP.group -> return GroupStep(newConfig, compound)
                NsP.forEach -> return ForEachStep(newConfig, compound)
                NsP.viewport -> return ViewportStep(newConfig, compound)
                NsP.choose -> return ChooseStep(newConfig, compound)
                NsP.`when` -> return ChooseWhenStep(newConfig, compound)
                NsP.otherwise -> return ChooseOtherwiseStep(newConfig, compound)
                NsP.`if` -> return ChooseStep(newConfig, compound)
                NsP.`try` -> return TryStep(newConfig, compound)
                NsP.catch -> return TryCatchStep(newConfig, compound)
                NsP.finally -> return TryFinallyStep(newConfig, compound)
                NsP.run -> return RunStep(newConfig, compound)
                else -> throw config.exception(XProcError.xiImpossible("Unsupported compound step type: ${compound.type}"))
            }
        }
    }

    final override val params = compound.params
    val inputPorts = mutableSetOf<String>()

    internal val runnableProviders = mutableMapOf<StepModel, () -> AbstractStep>()
    internal val edges = compound.edges
    internal val runnables = mutableListOf<AbstractStep>()
    internal val head = CompoundStepHead(config, compound.head)
    internal val foot = CompoundStepFoot(config, compound.foot)
    internal var stepName: String? = null
    internal var stepType: QName? = null

    override val readyToRun: Boolean
        get() = head.readyToRun

    init {
        inputPorts.addAll(compound.inputs.keys)
        head.staticOptions.putAll(staticOptions)

        runnableProviders[compound.head] = { head }
        runnableProviders[compound.foot] = { foot }
        for (step in compound.steps) {
            val stepRunnable = step.runnable(config)
            runnableProviders[step] = stepRunnable
        }
    }

    override fun instantiate() {
        val runnableMap = mutableMapOf<StepModel, AbstractStep>()
        for ((model, provider) in runnableProviders) {
            val stepRunnable = provider()
            runnableMap[model] = stepRunnable
            if (stepRunnable !is CompoundStepHead && stepRunnable !is CompoundStepFoot) {
                runnables.add(stepRunnable)
            }
        }

        for (edge in edges) {
            val from = runnableMap[edge.fromStep]!!
            val to = runnableMap[edge.toStep]!!

            val rfrom = if (from is CompoundStep) {
                from.foot
            } else {
                from
            }

            val rto = if (to is CompoundStep) {
                to.head
            } else {
                to
            }

            rfrom.receiver[edge.fromPort] = Pair(rto, edge.toPort)
            if (from is ViewportStep || from is TryStep || from is ChooseWhenStep) {
                from.foot.holdPorts.add(edge.fromPort)
            }
        }
    }

    override fun prepare() {
        if (runnables.isEmpty()) {
            instantiate()
        }
    }

    override fun run() {
    }

    override fun input(port: String, doc: XProcDocument) {
        throw UnsupportedOperationException("Never send an input to a compound step")
    }

    override fun output(port: String, document: XProcDocument) {
        throw UnsupportedOperationException("Never send an output to a compound step")
    }

    override fun close(port: String) {
        throw UnsupportedOperationException("Never close an input on a compound step")
    }

    override fun reset() {
        super.reset()
        head.reset()
        foot.reset()
        for (step in runnables) {
            step.reset()
        }
    }

    open fun runStepsExhaustively(steps: List<AbstractStep>): List<AbstractStep> {
        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(steps)

        // When we're running a step defined by a pipeline, the options passed in or computed
        // must be available for computing subsequent options.
        val atomicOptionValues = mutableMapOf<QName, LazyValue>()

        var done = false
        while (!done) {
            done = true
            var runMe: AbstractStep? = null
            for (step in stepsToRun) {
                if (step.readyToRun) {
                    runMe = step
                    stepsToRun.remove(step)
                    break
                }
            }
            if (runMe != null) {
                done = false

                if (runMe is AtomicOptionStep) {
                    runMe.atomicOptionValues.putAll(atomicOptionValues)
                    runMe.runStep()
                    if (runMe.externalValue == null) {
                        atomicOptionValues[runMe.externalName] = LazyValue(runMe.stepConfig, (runMe.implementation as ExpressionStep).expression, stepConfig)
                    } else {
                        atomicOptionValues[runMe.externalName] = LazyValue(runMe.stepConfig, runMe.externalValue!!.value, stepConfig)
                    }
                } else {
                    runMe.runStep()
                }
            }
        }

        return stepsToRun
    }

    override fun toString(): String {
        val dispType = stepType ?: type
        val dispName = stepName ?: name
        return "${dispType}/${dispName} (${id})"
    }
}