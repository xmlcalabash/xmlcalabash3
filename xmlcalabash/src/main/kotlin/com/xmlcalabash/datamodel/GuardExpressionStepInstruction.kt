package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx

class GuardExpressionStepInstruction(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.guard) {
    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}