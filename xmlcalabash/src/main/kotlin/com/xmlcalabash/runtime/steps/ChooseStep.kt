package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class ChooseStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    init {
        head.openPorts.remove("!context") // doesn't count as an open port from the outside
    }

    override fun run() {
        super.run()

        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val context = cache["!source"] ?: mutableListOf()
        cache.remove("!source")

        stepsToRun.clear()
        stepsToRun.addAll(runnables.filter { it !is ChooseWhenStep })

        stepConfig.environment.newExecutionContext(stepConfig)

        head.cacheInputs(cache)
        for (doc in context) {
            head.input("!context", doc)
        }

        head.runStep()

        runSubpipeline()

        for (step in runnables.filterIsInstance<ChooseWhenStep>()) {
            val guard = step.evaluateGuardExpression()
            if (guard) {
                step.runStep()
                break
            }
        }

        foot.runStep()
        stepConfig.environment.releaseExecutionContext()
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("!context")
    }
}