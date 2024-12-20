package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP

class OtherwiseInstruction(parent: ChooseInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.otherwise) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '0', NsP.output to '*')

    override fun elaborateInstructions() {
        if (depends.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsAttributeForbidden(Ns.depends))
        }
        super.elaborateInstructions()
    }
}