package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.RuntimeCompoundStep
import com.xmlcalabash.runtime.RuntimeGuardStep
import com.xmlcalabash.runtime.RuntimeStep

class WhenStep(pipelineConfig: XProcRuntime): RuntimeCompoundStep(pipelineConfig) {
    protected val stepsToRun = mutableListOf<RuntimeStep>()

    internal fun evaluateTestExpression(): Boolean {
        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                head.input(port, document)
            }
        }
        inputDocuments.clear()
        head.run()

        // Run everything up to the first expression...and any runnable splitters and joiners
        var guardStep: RuntimeGuardStep? = null
        val guardSteps = mutableListOf<RuntimeStep>()
        var found = false
        for (step in steps) {
            if (found) {
                if ((step.tag == NsCx.splitter || step.tag == NsCx.joiner) && step.readyToRun) {
                    guardSteps.add(step)
                } else {
                    stepsToRun.add(step)
                }
            } else {
                guardSteps.add(step)
            }
            if (step.tag == NsCx.guard) {
                guardStep = step as RuntimeGuardStep
                found = true
            }
        }

        var more = true
        while (more) {
            more = false
            val remaining = runTheseStepsExhaustively(guardSteps)
            if (remaining.isNotEmpty()) {
                guardSteps.clear()
                guardSteps.addAll(remaining)

                for (step in stepsToRun.toList()) {
                    if ((step.tag == NsCx.splitter || step.tag == NsCx.joiner) && step.readyToRun) {
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

        return guardStep!!.effectiveBooleanValue()
    }

    override fun runStep() {
        val remaining = runTheseStepsExhaustively(stepsToRun)
        if (remaining.isNotEmpty()) {
            throw XProcError.xiNoRunnableSteps().exception()
        }
    }
}