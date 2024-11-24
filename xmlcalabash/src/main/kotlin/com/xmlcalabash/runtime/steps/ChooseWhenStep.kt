package com.xmlcalabash.runtime.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.steps.internal.GuardStep

open class ChooseWhenStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): CompoundStep(yconfig, compound) {
    protected val stepsToRun = mutableListOf<AbstractStep>()

    open fun evaluateGuardExpression(): Boolean {
        if (runnables.isEmpty()) {
            instantiate()
        }

        stepConfig.newExecutionContext(stepConfig)

        stepsToRun.clear()

        head.runStep()

        // Run everything up to the first expression...and any runnable splitters and joiners
        var guardStep: AbstractStep? = null
        val guardSteps = mutableListOf<AbstractStep>()
        var found = false
        for (step in runnables) {
            if (found) {
                if ((step.type == NsCx.splitter || step.type == NsCx.joiner) && step.readyToRun) {
                    guardSteps.add(step)
                } else {
                    stepsToRun.add(step)
                }
            } else {
                guardSteps.add(step)
            }
            if (step.type == NsCx.guard) {
                guardStep = step
                found = true
            }
        }

        var more = true
        while (more) {
            more = false
            val remaining = runStepsExhaustively(guardSteps)
            if (remaining.isNotEmpty()) {
                guardSteps.clear()
                guardSteps.addAll(remaining)

                for (step in stepsToRun.toList()) {
                    if ((step.type == NsCx.splitter || step.type == NsCx.joiner) && step.readyToRun) {
                        guardSteps.add(step)
                        stepsToRun.remove(step)
                        more = true
                    }
                }

                if (!more) {
                    throw XProcError.xiNoRunnableSteps().exception()
                }
            }
        }

        stepConfig.releaseExecutionContext()

        return ((guardStep as AtomicStep).implementation as GuardStep).effectiveBooleanValue()
    }

    override fun run() {
        stepConfig.newExecutionContext(stepConfig)

        val left = runStepsExhaustively(stepsToRun)
        if (left.isNotEmpty()) {
            throw RuntimeException("did not run all steps")
        }

        for ((port, documents) in foot.cache) {
            for (document in documents) {
                foot.write(port, document)
            }
        }

        foot.runStep()
        stepConfig.releaseExecutionContext()
    }
}
