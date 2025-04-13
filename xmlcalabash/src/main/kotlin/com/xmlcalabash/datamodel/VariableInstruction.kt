package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import java.net.URI

open class VariableInstruction(parent: XProcInstruction, name: QName, stepConfig: InstructionConfiguration): VariableBindingContainer(parent, name, stepConfig, NsP.variable) {
    override fun elaborateInstructions() {
        if (select == null) {
            throw stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.select))
        } else {
            asType = asType ?: stepConfig.typeUtils.parseSequenceType("item()*")
            select = select!!.cast(asType!!)
        }

        super.elaborateInstructions()

        if (name.namespaceUri == NsP.namespace) {
            throw stepConfig.exception(XProcError.xsOptionInXProcNamespace(name))
        }
    }
}