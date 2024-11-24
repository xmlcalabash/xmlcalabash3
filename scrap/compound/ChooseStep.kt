package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.RuntimeCompoundStep
import com.xmlcalabash.runtime.RuntimeWhenStep

class ChooseStep(pipelineConfig: XProcRuntime): RuntimeCompoundStep(pipelineConfig) {

    override fun runStep() {
        val exec = stepConfig.newExecutionContext()
        stepConfig.setExecutionContext(exec)

        if (inputDocuments.containsKey("!source")) {
            val sequence = inputDocuments["!source"]!!
            inputDocuments.remove("!source")
            for (doc in sequence) {
                head.input("!context", doc)
            }
        }

        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                head.input(port, document)
            }
        }

        head.run()
        head.receiver.close()

        val remaining = runTheseStepsExhaustively(steps.filter { it !is RuntimeWhenStep })
        if (remaining.isNotEmpty()) {
            throw XProcError.xiNoRunnableSteps().exception()
        }

        for (step in steps.filterIsInstance<RuntimeWhenStep>()) {
            val guard = step.evaluateTestExpression()
            if (guard) {
                step.run()
                break
            }
        }

        inputDocuments.clear()
        stepConfig.runtime.releaseExecutionContext()
    }

    override fun connectReceivers() {
        super.connectReceivers()
        for (step in steps.filterIsInstance<RuntimeWhenStep>()) {
            for ((portName, _) in step.subpipeline.outputManifold) {
                step.receiver.addDispatcher(portName, foot, portName)
            }
        }
    }
}