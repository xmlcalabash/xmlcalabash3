package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.XdmNode

class ImportNode(parent: AnyNode, node: XdmNode): ElementNode(parent, node) {
    val href = node.baseURI.resolve(node.getAttributeValue(Ns.href)!!)
    var firstPass = true

    override fun computeUseWhenOnThisElement(context: UseWhenContext) {
        super.computeUseWhenOnThisElement(context)
        if (firstPass) {
            firstPass = false
        } else {
            if (useWhen == null) {
                useWhen = false
                context.useWhen.remove(this)
                context.resolvedCount++
            }
        }
    }
}