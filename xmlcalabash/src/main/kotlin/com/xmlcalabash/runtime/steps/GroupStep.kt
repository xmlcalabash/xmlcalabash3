package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class GroupStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(runnables)

        stepConfig.environment.newExecutionContext(stepConfig)

        head.runStep()

        val left = runStepsExhaustively(stepsToRun)
        if (left.isNotEmpty()) {
            throw RuntimeException("did not run all steps")
        }

        foot.runStep()
        stepConfig.environment.releaseExecutionContext()
    }
}