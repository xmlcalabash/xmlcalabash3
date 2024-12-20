package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

class CatchInstruction(parent: XProcInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.catch) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '0', NsP.output to '*')

    private val _code = mutableListOf<QName>()
    var code: List<QName>
        get() = _code
        set(value) {
            checkOpen()
            _code.clear()
            _code.addAll(value)
        }

    override fun elaborateInstructions() {
        if (depends.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsAttributeForbidden(Ns.depends))
        }
        super.elaborateInstructions()
    }

    override fun checkInputBindings() {
        for (child in children.filterIsInstance<InputBindingInstruction>()) {
            if (child.port != "error" && child.children.isEmpty()) {
                throw stepConfig.exception(XProcError.xsNotConnected(child.port))
            }
        }
    }

    override fun findDefaultReadablePort(drp: PortBindingContainer?) {
        // Make this one "by hand" because p:catch can't have one input instructions
        // It's a sequence because it might have zero inputs
        var found = false
        for (child in children.filterIsInstance<InputInstruction>()) {
            found = found || child.port == "error"
        }

        if (!found) {
            val error = InputInstruction(this, "error", true, true)
            if (_children.isEmpty()) {
                _children.add(error)
            } else {
                _children.add(1, error)
            }
        }

        super.findDefaultReadablePort(drp)
    }

}