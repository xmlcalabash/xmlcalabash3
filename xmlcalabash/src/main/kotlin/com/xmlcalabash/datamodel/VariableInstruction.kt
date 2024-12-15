package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import java.net.URI

open class VariableInstruction(parent: XProcInstruction, name: QName, stepConfig: InstructionConfiguration): VariableBindingContainer(parent, name, stepConfig, NsP.variable) {
    override fun elaborateInstructions() {
        if (select == null) {
            throw XProcError.xsMissingRequiredAttribute(Ns.select).exception()
        } else {
            asType = asType ?: stepConfig.parseSequenceType("item()*")
            select = select!!.cast(asType!!)
        }

        super.elaborateInstructions()

        if (name.namespaceUri == NsP.namespace) {
            throw XProcError.xsOptionInXProcNamespace(name).exception()
        }
    }
}