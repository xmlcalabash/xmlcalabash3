package com.xmlcalabash.datamodel

import net.sf.saxon.s9api.QName

abstract class ConnectionInstruction(parent: XProcInstruction, instructionType: QName): XProcInstruction(parent, parent.stepConfig.copy(), instructionType) {
    internal abstract fun promoteToStep(parent: StepDeclaration, step: StepDeclaration): List<AtomicStepInstruction>
}