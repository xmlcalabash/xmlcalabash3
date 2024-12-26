package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName

abstract class CompoundLoopDeclaration(parent: XProcInstruction, instructionType: QName): CompoundStepDeclaration(parent, parent.stepConfig.copy(), instructionType) {
    override fun findDefaultReadablePort(drp: PortBindingContainer?) {
        if (children.filterIsInstance<InputInstruction>().isEmpty()) {
            // Make this one "by hand" because p:for-each can't have one input instructions.
            // But don't make it twice.
            // It's a sequence because it might have zero inputs
            val current = InputInstruction(this, "current", true, true)
            if (children.isEmpty()) {
                _children.add(current)
            } else {
                _children.add(1, current)
            }
        }

        super.findDefaultReadablePort(drp)
    }

    override fun elaborateInstructions() {
        resolveAnonymousWithInput()
        super.elaborateInstructions()
    }

    override fun defaultReadablePort(): PortBindingContainer? {
        return namedInput("current")
    }

    override fun checkInputBindings() {
        for (child in children.filterIsInstance<InputBindingInstruction>()) {
            if (child.port != "current" && !child.weldedShut && child.children.isEmpty()) {
                throw stepConfig.exception(XProcError.xsNotConnected(child.port))
            }
        }
    }
}