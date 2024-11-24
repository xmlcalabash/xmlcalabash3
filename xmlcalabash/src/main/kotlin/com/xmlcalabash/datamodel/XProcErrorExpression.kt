package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue

class XProcErrorExpression private constructor(stepConfig: StepConfiguration): XProcExpression(stepConfig, SequenceType.ANY, false, emptyList()) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration): XProcErrorExpression {
            return XProcErrorExpression(stepConfig)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        return error(stepConfig)
    }

    override fun xevaluate(): () -> XdmValue {
        return { evaluate() }
    }

    override fun evaluate(): XdmValue {
        throw XProcError.xiImpossible("Attempt to evaluate error expression").exception()
    }

    override fun computeStaticValue(stepConfig: StepConfiguration): XdmValue? {
        throw XProcError.xiImpossible("Attempt to find static value of error expression").exception()
    }

    override fun toString(): String {
        return "ERROR"
    }
}