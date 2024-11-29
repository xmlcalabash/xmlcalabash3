package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmNode

class OptionNode(parent: AnyNode, node: XdmNode, val name: QName, val select: String): ElementNode(parent, node) {
    var visible = true
    var asType: SequenceType? = null

    init {
        visible = node.getAttributeValue(Ns.visibility) != "private"

        val typeStr = node.getAttributeValue(Ns.asType)
        if (typeStr != null) {
            asType = stepConfig.parseSequenceType(typeStr)
        }
    }

    override fun resolveUseWhen(context: UseWhenContext) {
        if (useWhen == false) {
            return
        }

        super.resolveUseWhen(context)

        if (context.staticOptions[this] == null) {
            if (useWhen == true) {
                val value = if (context.builder.staticOptionsManager.useWhenOptions.containsKey(name)) {
                    context.builder.staticOptionsManager.useWhenOptions[name]
                } else {
                    context.resolveExpression(stepConfig, select)
                }

                if (value != null && asType != null) {
                    if (!asType!!.matches(value)) {
                        throw XProcError.xdBadType(value.toString(), asType!!.underlyingSequenceType.toString()).exception()
                    }
                }

                context.staticOptions[this] = value
            } else {
                context.staticOptions[this] = null
            }
        }
    }
}