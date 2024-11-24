package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx

class AtomicSelectStepInstruction(parent: XProcInstruction, val select: XProcExpression): AtomicStepInstruction(parent, NsCx.select) {
    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}