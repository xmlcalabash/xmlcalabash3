package com.xmlcalabash.datamodel

import net.sf.saxon.s9api.QName

open class InputBindingInstruction(parent: XProcInstruction, stepConfig: StepConfiguration, instructionType: QName): PortBindingContainer(parent, stepConfig, instructionType) {
    internal val defaultBindings = mutableListOf<ConnectionInstruction>()
}