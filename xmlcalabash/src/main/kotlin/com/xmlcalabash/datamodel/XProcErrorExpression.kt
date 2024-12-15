package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue

class XProcErrorExpression private constructor(stepConfig: XProcStepConfiguration): XProcExpression(stepConfig, SequenceType.ANY, false, emptyList()) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration): XProcErrorExpression {
            return XProcErrorExpression(stepConfig)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        return error(stepConfig)
    }

    override fun xevaluate(config: XProcStepConfiguration): () -> XdmValue {
        return { evaluate(config) }
    }

    override fun evaluate(config: XProcStepConfiguration): XdmValue {
        throw UnsupportedOperationException("Attempt to evaluate error expression")
    }

    override fun computeStaticValue(stepConfig: InstructionConfiguration): XdmValue? {
        throw UnsupportedOperationException("Attempt to find static value of error expression")
    }

    override fun toString(): String {
        return "ERROR"
    }
}