package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class DeclareStepNode(parent: AnyNode, node: XdmNode): RootNode(parent, parent.stepConfig, node) {
    companion object {
        private val preamble = setOf(NsP.input, NsP.output, NsP.option)
    }
    var type: QName? = null
    var visible: Boolean = true
    val isAtomic: Boolean
        get() {
            for (child in children.filterIsInstance<ElementNode>()) {
                if (child.useWhen != false) {
                    if (child.node.nodeName !in preamble) {
                        return false
                    }
                }
            }
            return true
        }

    init {
        if (node.getAttributeValue(Ns.visibility) == "private") {
            visible = false
        }
        val typestr = node.getAttributeValue(Ns.type)
        if (typestr != null) {
            type = stepConfig.parseQName(typestr)
        } else {
            visible = false
        }
    }
}