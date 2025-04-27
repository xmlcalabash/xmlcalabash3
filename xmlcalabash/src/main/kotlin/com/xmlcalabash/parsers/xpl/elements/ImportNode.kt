package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.XdmNode

class ImportNode(parent: AnyNode, node: XdmNode): ElementNode(parent, node) {
    val href = UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!
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