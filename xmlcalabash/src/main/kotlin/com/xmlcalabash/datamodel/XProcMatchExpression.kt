package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue

class XProcMatchExpression private constructor(stepConfig: XProcStepConfiguration, val match: String): XProcExpression(stepConfig, SequenceType.ANY, false, emptyList()) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, match: String): XProcMatchExpression {
            return XProcMatchExpression(stepConfig, match)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        throw UnsupportedOperationException("XProcMatchExpression cannot be cast")
    }

    override fun xevaluate(config: XProcStepConfiguration): () -> XdmValue {
        return { evaluate(config) }
    }

    override fun evaluate(config: XProcStepConfiguration): XdmValue {
        var map = XdmMap()
        map = map.put(XdmAtomicValue("match"), XdmAtomicValue(match))
        for ((name, value) in variableBindings) {
            map = map.put(XdmAtomicValue(name), value)
        }
        return map
    }

    override fun toString(): String {
        return match
    }
}