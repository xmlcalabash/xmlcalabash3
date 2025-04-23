package com.xmlcalabash.datamodel

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.ValueTemplate
import net.sf.saxon.s9api.*
import net.sf.saxon.type.StringConverter.StringToUntypedAtomic

class XProcAvtExpression private constructor(stepConfig: StepConfiguration, val avt: ValueTemplate, asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression(stepConfig, asType, false, values) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, avt: ValueTemplate, asType: SequenceType = SequenceType.ANY, values: List<XdmAtomicValue> = emptyList()): XProcAvtExpression {
            return XProcAvtExpression(stepConfig, avt, asType, values)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        if (details.error == null) {
            return avt(stepConfig, avt, asType, values)
        }
        return this
    }

    override fun xevaluate(config: StepConfiguration): () -> XdmValue {
        return { evaluate(config) }
    }

    override fun evaluate(config: StepConfiguration): XdmValue {
        val sb = StringBuilder()

        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                sb.append(avt.value[index])
            } else {
                val compiler = config.newXPathCompiler()
                for (name in variableRefs) {
                    compiler.declareVariable(name)
                }

                val selector = compiler.compile(avt.value[index]).load()
                //selector.resourceResolver = stepConfiguration.pipelineConfig.documentManager

                setupExecutionContext(config, selector)
                val result = try {
                    for ((name, value) in variableBindings) {
                        selector.setVariable(name, value)
                    }
                    selector.evaluate()
                } finally {
                    teardownExecutionContext()
                }

                // Check that all the parts are ok, but rely on sequence construction from the top
                // level (so that we get the correct separators, for example).
                for (item in result.iterator()) {
                    if (!(item is XdmNode || item is XdmAtomicValue)) {
                        throw stepConfig.exception(XProcError.xdInvalidAvtResult(avt.value[index]))
                    }
                }

                sb.append(result.underlyingValue.stringValue)
            }
        }

        if (asType !== SequenceType.ANY || values.isNotEmpty()) {
            // This must be an attribute, so treat the value as untyped atomic to begin with
            val value = StringToUntypedAtomic().convert(XdmAtomicValue(sb.toString()).underlyingValue)
            return config.typeUtils.checkType(null, XdmAtomicValue(value), asType, values)
        }

        return XdmAtomicValue(sb.toString())
    }

    override fun toString(): String {
        return avt.toString()
    }
}