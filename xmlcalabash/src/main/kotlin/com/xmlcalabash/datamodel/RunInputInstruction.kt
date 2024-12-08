package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns

class RunInputInstruction(parent: XProcInstruction, stepConfig: InstructionConfiguration): WithInputInstruction(parent, stepConfig) {
    override fun elaborateInstructions() {
        super.elaborateInstructions()

        if (!portDefined) {
            throw XProcError.xsMissingRequiredAttribute(Ns.port).exception()
        }

        _sequence = true
    }
}