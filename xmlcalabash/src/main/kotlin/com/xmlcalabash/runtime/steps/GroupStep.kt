package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class GroupStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): CompoundStep(yconfig, compound) {
    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(runnables)

        stepConfig.newExecutionContext(stepConfig)

        head.runStep()

        val left = runStepsExhaustively(stepsToRun)
        if (left.isNotEmpty()) {
            throw RuntimeException("did not run all steps")
        }

        foot.runStep()
        stepConfig.releaseExecutionContext()
    }
}