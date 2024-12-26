package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class ForEachStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    init {
        head.openPorts.remove("current") // doesn't count as an open port from the outside
        foot.alwaysAllowSequences = true
    }

    override fun run() {
        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val sequence = mutableListOf<XProcDocument>()
        sequence.addAll(cache["!source"] ?: emptyList())
        cache.remove("!source")

        var position = 1L

        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        val exec = stepConfig.environment.newExecutionContext(stepConfig)
        exec.iterationSize = sequence.size.toLong()

        if (sequence.isEmpty()) {
            head.runStep()
            foot.runStep()
        } else {
            while (sequence.isNotEmpty()) {
                exec.iterationPosition = position
                if (position > 1) {
                    head.reset()
                    head.showMessage = false
                    foot.reset()
                    for (step in stepsToRun) {
                        step.reset()
                    }
                }

                head.cacheInputs(cache)

                head.input("current", sequence.removeFirst())

                head.runStep()

                runSubpipeline()

                foot.runStep()
                position++
            }
        }

        cache.clear()
        stepConfig.environment.releaseExecutionContext()
    }

    override fun abort() {
        super.abort()
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("current")
    }
}