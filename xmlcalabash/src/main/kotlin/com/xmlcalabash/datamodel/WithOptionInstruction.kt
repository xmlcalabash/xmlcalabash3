package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.ma.arrays.ArrayItemType
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

            if (select is XProcShortcutExpression) {
                val shortcut = select as XProcShortcutExpression
                // This is a hack. When parsing an XProc grammar, we don't know the types of the options.
                // An option shortcut is an AVT, *unless* the underlying type is a map or array.
                // Now we know the underlying type...
                val primaryType = asType!!.underlyingSequenceType.primaryType
                if (primaryType is MapType || primaryType is ArrayItemType) {
                    val expr = XProcExpression.select(shortcut.stepConfig, shortcut.shortcut, asType!!, shortcut.collection, optionValues)
                    _select = expr
                } else {
                    val avt = XProcExpression.avt(shortcut.stepConfig, shortcut.shortcut, asType!!, optionValues)
                    _select = avt
                }
            }

            select = select!!.cast(asType!!, optionValues)
        }

        super.elaborateInstructions()
    }
}