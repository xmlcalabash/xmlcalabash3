package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class GroupStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    override fun run() {
        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        stepConfig.environment.newExecutionContext(stepConfig)

        head.runStep()
        runSubpipeline()
        foot.runStep()

        stepConfig.environment.releaseExecutionContext()
    }
}