package com.xmlcalabash.runtime.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.steps.internal.GuardStep

open class ChooseWhenStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    protected val localStepsToRun = mutableListOf<AbstractStep>()

    open fun evaluateGuardExpression(): Boolean {
        if (runnables.isEmpty()) {
            instantiate()
        }

        stepConfig.environment.newExecutionContext(stepConfig)

        localStepsToRun.clear()
        localStepsToRun.addAll(runnables)

        // Run everything up to, and including, the guard expression,
        // leave everything else to be run after, if the expression is true
        var guardStep: AbstractStep? = null
        val guardSteps = mutableListOf<AbstractStep>()
        while (localStepsToRun.isNotEmpty()) {
            val step = localStepsToRun.removeFirst()
            guardSteps.add(step)
            if (step.type == NsCx.guard) {
                guardStep = step
                break
            }
        }

        head.runStep()

        stepsToRun.clear()
        stepsToRun.addAll(guardSteps)
        runSubpipeline()

        stepConfig.environment.releaseExecutionContext()

        return ((guardStep as AtomicStep).implementation as GuardStep).effectiveBooleanValue()
    }

    override fun run() {
        stepConfig.environment.newExecutionContext(stepConfig)

        stepsToRun.clear()
        stepsToRun.addAll(localStepsToRun)
        runSubpipeline()

        for ((port, documents) in foot.cache) {
            for (document in documents) {
                foot.write(port, document)
            }
        }

        foot.runStep()
        stepConfig.environment.releaseExecutionContext()
    }
}
