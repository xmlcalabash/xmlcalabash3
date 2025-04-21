package com.xmlcalabash.runtime.steps

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.steps.internal.GuardStep

open class ChooseWhenStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    protected val localStepsToRun = mutableListOf<AbstractStep>()

    open fun evaluateGuardExpression(parent: CompoundStep): Boolean {
        if (runnables.isEmpty()) {
            instantiate()
        }

        runtimeParent = parent
        stepConfig.saxonConfig.newExecutionContext(this)

        try {
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

            head.runStep(this)

            stepsToRun.clear()
            stepsToRun.addAll(guardSteps)
            runSubpipeline()
            return ((guardStep as AtomicStep).implementation as GuardStep).effectiveBooleanValue()
        } finally {
            stepConfig.saxonConfig.releaseExecutionContext()
        }
    }

    override fun run() {
        stepConfig.saxonConfig.newExecutionContext(stepConfig)
        try {
            stepsToRun.clear()
            stepsToRun.addAll(localStepsToRun)
            runSubpipeline()

            for ((port, documents) in foot.cache) {
                for (document in documents) {
                    foot.write(port, document)
                }
            }

            foot.runStep(this)
        } finally {
            stepConfig.saxonConfig.releaseExecutionContext()
        }
    }
}
