package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName

class AtomicExpressionStepInstruction(parent: XProcInstruction, val expression: XProcExpression): AtomicStepInstruction(parent, NsCx.expression) {
    internal var externalName: QName? = null

    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}