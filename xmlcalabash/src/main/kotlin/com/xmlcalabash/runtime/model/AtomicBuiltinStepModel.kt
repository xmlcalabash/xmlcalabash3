package com.xmlcalabash.runtime.model

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.datamodel.*
import com.xmlcalabash.graph.AtomicModel
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.parameters.*
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.AtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType

open class AtomicBuiltinStepModel(runtime: XProcRuntime, val model: AtomicModel): AtomicStepModel(runtime, model) {
    internal val provider: () -> XProcStep
    internal lateinit var params: RuntimeStepParameters

    init {
        val childStep = model.step as AtomicStepInstruction

        staticOptions.putAll(childStep.staticOptions)
        extensionAttributes.putAll(childStep.extensionAttributes)

        provider = when (childStep.instructionType) {
            NsCx.empty -> {
                params = EmptyStepParameters(name, location, inputs, outputs, options)
                stepConfig.stepProvider(params)
            }

            NsCx.inline -> {
                val step = childStep as AtomicInlineStepInstruction
                params = InlineStepParameters(name, location, inputs, outputs, options,
                    step.filter, step.contentType, step.encoding)
                stepConfig.stepProvider(params)
            }

            NsCx.document -> {
                val step = childStep as AtomicDocumentStepInstruction
                params = DocumentStepParameters(name, location, inputs, outputs, options, step.contentType)
                stepConfig.stepProvider(params)
            }

            NsCx.select -> {
                val step = childStep as AtomicSelectStepInstruction
                val param = SelectStepParameters(name, location, inputs, outputs, options, step.select)
                stepConfig.stepProvider(param)
            }

            NsCx.expression -> {
                val step = childStep as AtomicExpressionStepInstruction

                val inscopeOptions = mutableMapOf<QName, RuntimeOption>()

                // Options is usually empty, but in-scope static references may appear here
                for ((name, option) in options) {
                    inscopeOptions[name] = option
                }

                for (name in step.expression.variableRefs) {
                    if (name in step.inscopeVariables) {
                        val expr = step.inscopeVariables[name]!!
                        inscopeOptions[name] = RuntimeOption(name, expr.asType ?: SequenceType.ANY, emptyList(), expr.select!!)
                    }
                }

                params = if (step.externalName != null) {
                    OptionStepParameters(name, location, inputs, outputs, inscopeOptions, step)
                } else {
                    ExpressionStepParameters(name, location, inputs, outputs, inscopeOptions, step)
                }

                stepConfig.stepProvider(params)
            }

            else -> {
                params = RuntimeStepParameters(childStep.instructionType, childStep.name, childStep.location,
                    inputs, outputs, options)
                stepConfig.stepProvider(params)
            }
        }
    }

    override fun runnable(yconfig: RuntimeStepConfiguration): () -> AbstractStep {
        return { AtomicStep(yconfig.newInstance(stepConfig), this) }
    }
}