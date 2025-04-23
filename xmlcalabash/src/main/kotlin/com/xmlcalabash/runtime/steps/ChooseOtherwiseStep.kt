package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class ChooseOtherwiseStep(yconfig: XProcStepConfiguration, compound: CompoundStepModel): ChooseWhenStep(yconfig, compound) {
    override fun evaluateGuardExpression(parent: CompoundStep): Boolean {
        if (runnables.isEmpty()) {
            instantiate()
        }

        localStepsToRun.clear()
        localStepsToRun.addAll(runnables)

        runtimeParent = parent
        head.runStep(this)

        return true
    }
}