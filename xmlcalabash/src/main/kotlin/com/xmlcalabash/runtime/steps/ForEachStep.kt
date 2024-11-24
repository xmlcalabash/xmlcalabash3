package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class ForEachStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): CompoundStep(yconfig, compound) {
    init {
        head.openPorts.remove("current") // doesn't count as an open port from the outside
        foot.alwaysAllowSequences = true
    }

    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val sequence = mutableListOf<XProcDocument>()
        sequence.addAll(cache["!source"] ?: emptyList())
        cache.remove("!source")

        var position = 1L

        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(runnables)

        val exec = stepConfig.newExecutionContext(stepConfig)
        exec.iterationSize = sequence.size.toLong()

        if (sequence.isEmpty()) {
            head.runStep()
            foot.runStep()
        } else {
            while (sequence.isNotEmpty()) {
                exec.iterationPosition = position
                if (position > 1) {
                    head.reset()
                    foot.reset()
                    for (step in stepsToRun) {
                        step.reset()
                    }
                }

                head.cacheInputs(cache)
                head.input("current", sequence.removeFirst())

                head.runStep()

                val left = runStepsExhaustively(stepsToRun)
                if (left.isNotEmpty()) {
                    throw RuntimeException("did not run all steps")
                }

                foot.runStep()
                position++
            }
        }

        cache.clear()
        stepConfig.releaseExecutionContext()
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("current")
    }
}