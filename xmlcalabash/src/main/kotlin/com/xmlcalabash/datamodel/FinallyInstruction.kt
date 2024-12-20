package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP

class FinallyInstruction(parent: XProcInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.finally) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '0', NsP.output to '*')
    private val errorPort = InputInstruction(this, "error", true, true)

    override fun elaborateInstructions() {
        if (depends.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsAttributeForbidden(Ns.depends))
        }
        // Make this one "by hand" because p:finally can't have an input instructions
        _children.add(1, errorPort)
        super.elaborateInstructions()
    }

    override fun checkInputBindings() {
        for (child in children.filterIsInstance<InputBindingInstruction>()) {
            if (child.port != "error" && child.children.isEmpty()) {
                throw stepConfig.exception(XProcError.xsNotConnected(child.port))
            }
        }
    }

    override fun defaultReadablePort(): PortBindingContainer? {
        return errorPort
    }

    override fun primaryInput(): PortBindingContainer? {
        return errorPort
    }
}