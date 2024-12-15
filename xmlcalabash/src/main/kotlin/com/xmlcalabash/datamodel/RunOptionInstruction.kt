package com.xmlcalabash.datamodel

import net.sf.saxon.s9api.QName

class RunOptionInstruction(parent: XProcInstruction, name: QName, stepConfig: InstructionConfiguration): WithOptionInstruction(parent, name, stepConfig) {
    internal var _static: Boolean? = null
    var static: Boolean?
        get() = _static
        set(value) {
            checkOpen()
            _static = value
        }
}