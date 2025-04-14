package com.xmlcalabash.datamodel

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.ma.arrays.ArrayItem
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue

class XProcConstantExpression private constructor(stepConfig: StepConfiguration, value: XdmValue, asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression(stepConfig, asType, false, values) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, value: XdmValue, asType: SequenceType = SequenceType.ANY, values: List<XdmAtomicValue> = emptyList()): XProcConstantExpression {
            if (asType !== SequenceType.ANY || values.isNotEmpty()) {
                stepConfig.typeUtils.checkType(null, value, asType, values)
            }
            return XProcConstantExpression(stepConfig, value, asType, values)
        }
    }

    init {
        _staticValue = value
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        return constant(stepConfig, staticValue!!, asType, values)
    }

    override fun xevaluate(config: StepConfiguration): () -> XdmValue {
        return { staticValue!! }
    }

    override fun evaluate(config: StepConfiguration): XdmValue {
        return config.typeUtils.checkType(null, staticValue!!, asType, values)
    }

    override fun computeStaticValue(stepConfig: InstructionConfiguration): XdmValue {
        checkedStatic = true
        return staticValue!!
    }

    override fun toString(): String {
        val value = _staticValue!!.underlyingValue
        when (value) {
            is MapItem -> {
                val size = value.size()
                if (size > 0) {
                    return "...map with ${size} keys..."
                }
                return "...empty map..."
            }
            is ArrayItem -> {
                val size = value.length
                if (size > 0) {
                    return "...array with ${size} items..."
                }
                return "...empty array..."
            }
            else -> return value.stringValue
        }
    }
}