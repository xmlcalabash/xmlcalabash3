package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx

class AtomicEmptyStepInstruction(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.empty) {
    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}