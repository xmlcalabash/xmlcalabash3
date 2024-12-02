package com.xmlcalabash.runtime.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

class PipelineStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): GroupStep(yconfig,compound) {
    override fun runStep() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        for (step in runnables.filterIsInstance<AtomicOptionStep>()) {
            if (step.externalName !in params.options) {
                // I don't think this can happen, but just in case...
                // If you attempt to pass an option that isn't expected on the step, just ignore it
                continue
            }

            if (step.externalName in staticOptions) {
                val value = staticOptions[step.externalName]!!.staticValue.evaluate()
                val document = XProcDocument.ofValue(value, stepConfig, MediaType.OCTET_STREAM, DocumentProperties())
                step.externalValue = document
            } else if (step.externalName in head.options) {
                val list = head.options[step.externalName]!!
                if (list.size != 1) {
                    throw XProcError.xiImpossible("Dynamic option is a collection?").exception()
                }
                step.externalValue = list.first()
            }
        }

        super.runStep()
    }
}