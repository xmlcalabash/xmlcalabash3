package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.RuntimeCompoundStep

class ForEachStep(pipelineConfig: XProcRuntime): RuntimeCompoundStep(pipelineConfig) {

    override fun runStep() {
        val sequence = inputDocuments["!source"]!!
        inputDocuments.remove("!source")

        var position = 1L

        val exec = stepConfig.newExecutionContext()
        stepConfig.setExecutionContext(exec)
        exec.iterationSize = sequence.size.toLong()

        while (sequence.isNotEmpty()) {
            exec.iterationPosition = position
            if (position > 1) {
                for (step in steps) {
                    step.reset()
                }
            }

            head.input("current", sequence.removeFirst())
            for ((port, documents) in inputDocuments) {
                for (document in documents) {
                    head.input(port, document)
                }
            }

            head.run()
            head.receiver.close()
            val remaining = runTheseStepsExhaustively(steps)
            if (remaining.isNotEmpty()) {
                throw XProcError.xiNoRunnableSteps().exception()
            }

            position++
        }

        inputDocuments.clear()
        stepConfig.runtime.releaseExecutionContext()
    }
}