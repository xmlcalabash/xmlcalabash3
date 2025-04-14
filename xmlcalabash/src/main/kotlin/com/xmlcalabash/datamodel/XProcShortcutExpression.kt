package com.xmlcalabash.datamodel

import com.xmlcalabash.config.StepConfiguration
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue

// When an option shortcut is encountered, we don't know if it's going to be an AVT or not...we need the declaration to tell

class XProcShortcutExpression private constructor(stepConfig: StepConfiguration, val shortcut: String, asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression(stepConfig, asType, false, values) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, shortcut: String, asType: SequenceType = SequenceType.ANY, values: List<XdmAtomicValue> = emptyList()): XProcShortcutExpression {
            return XProcShortcutExpression(stepConfig, shortcut, asType, values)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        throw UnsupportedOperationException()
    }

    override fun xevaluate(config: StepConfiguration): () -> XdmValue {
        throw UnsupportedOperationException("Cannot evaluate a shortcut expression")
    }

    override fun evaluate(config: StepConfiguration): XdmValue {
        throw UnsupportedOperationException("Cannot evaluate a shortcut expression")
    }

    override fun toString(): String {
        return shortcut
    }
}