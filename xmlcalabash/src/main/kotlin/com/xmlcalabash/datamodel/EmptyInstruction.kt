package com.xmlcalabash.datamodel

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP

class EmptyInstruction(parent: XProcInstruction) : ConnectionInstruction(parent, NsP.empty) {

    override fun promoteToStep(parent: StepDeclaration, step: StepDeclaration): List<AtomicStepInstruction> {
        val emptyStep = AtomicStepInstruction(parent, NsCx.empty)
        emptyStep.depends.addAll(step.depends)

        emptyStep.elaborateInstructions()

        val output = emptyStep.withOutput()
        output.port = "result"
        output.primary = true
        output.sequence = true
        output.contentTypes = MediaType.MATCH_ANY

        return listOf(emptyStep)
    }
}