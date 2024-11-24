package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class ChooseOtherwiseStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): ChooseWhenStep(yconfig, compound) {
    override fun evaluateGuardExpression(): Boolean {
        if (runnables.isEmpty()) {
            instantiate()
        }

        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        head.runStep()

        return true
    }
}