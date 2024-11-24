package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.steps.internal.ExpressionStep
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class RuntimePipelineStep(pipelineConfig: XProcRuntime): RuntimeSubpipelineStep(pipelineConfig) {
    private val externalOptions = mutableMapOf<QName,XProcDocument>()
    fun option(name: QName, value: XProcDocument) {
        externalOptions[name] = value
    }

    val staticOptions: Set<QName>
        get() {
            val sopt = mutableSetOf<QName>()
            for ((name, model) in subpipeline.optionManifold) {
                if (model.static) {
                    sopt.add(name)
                }
            }
            return sopt
        }

    override fun runStep() {
        val externalOverrides = mutableMapOf<QName, ExpressionStep>()
        for (step in subpipeline.steps.filterIsInstance<RuntimeOptionStep>()) {
            externalOverrides[step.optionName] = step.implementation as ExpressionStep
        }

        val pipelineOptions = mutableMapOf<QName, XProcDocument>()
        for ((name, value) in externalOptions) {
            val model = subpipeline.optionManifold[name] ?: throw XProcError.xsNoSuchOption(name).exception()
            if (model.static) {
                logger.warn { "Static options cannot be set at runtime: ${name}"}
            } else {
                val checkedValue = value.with(stepConfig.checkType(name, value.value, model.asType, model.values))
                pipelineOptions[name] = checkedValue
                //externalOverrides[name]?.setExternalValue(checkedValue)
            }
        }

        subpipeline.run()
        subpipeline.foot.forwardOutputs(runtime)
    }

}
