package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue

open class WithOptionInstruction(parent: XProcInstruction, name: QName, stepConfig: StepConfiguration): VariableBindingContainer(parent, name, stepConfig, NsP.withOption) {
    internal var optionValues: List<XdmAtomicValue> = emptyList()
    internal var initializer: String? = null

    override fun elaborateInstructions() {
        if (select == null) {
            throw XProcError.xsMissingRequiredAttribute(Ns.select).exception()
        } else {
            asType = asType ?: stepConfig.parseSequenceType("item()*")

            // This is a hack. When parsing an XProc grammar, we don't know the types of the options.
            // An option shortcut is an AVT, *unless* the underlying type is a map or array.
            // Now we know the underlying type, patch if we have to.
            if (asType!!.underlyingSequenceType.primaryType is MapType && select is XProcAvtExpression) {
                val avt = select as XProcAvtExpression
                val expr = XProcExpression.select(avt.stepConfig, avt.toString(), asType!!, avt.collection, optionValues)
                _select = expr
            }

            select = select!!.cast(asType!!, optionValues)
        }

        super.elaborateInstructions()
    }
}