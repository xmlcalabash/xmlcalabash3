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
                    localStepsToRun.add(step)
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
            val remaining = try {
                runStepsExhaustively(guardSteps)
            } catch (ex: XProcException) {
                if (ex.error.code == NsErr.threadInterrupted) {
                    for (step in stepsToRun) {
                        step.abort()
                    }
                }
                throw ex
            }

            if (remaining.isNotEmpty()) {
                guardSteps.clear()
                guardSteps.addAll(remaining)

                for (step in localStepsToRun.toList()) {
                    if ((step.type == NsCx.splitter || step.type == NsCx.joiner) && step.readyToRun) {
                        guardSteps.add(step)
                        localStepsToRun.remove(step)
                        more = true
                    }
                }

                if (!more) {
                    throw stepConfig.exception(XProcError.xiNoRunnableSteps())
                }
            }
        }

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
