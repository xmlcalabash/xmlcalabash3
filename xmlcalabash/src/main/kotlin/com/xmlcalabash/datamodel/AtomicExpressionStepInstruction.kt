package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName

class AtomicExpressionStepInstruction(parent: XProcInstruction, val expression: XProcExpression): AtomicStepInstruction(parent, NsCx.expression) {
    private var _externalName: QName? = null

    internal var externalName: QName?
        get() = _externalName
        set(value) {
            _externalName = value
        }

    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}