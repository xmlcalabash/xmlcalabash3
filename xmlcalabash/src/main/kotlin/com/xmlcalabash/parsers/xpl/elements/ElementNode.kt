package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class ElementNode(parent: AnyNode?, stepConfig: StepConfiguration, node: XdmNode): AnyNode(parent, stepConfig, node) {
    constructor(parent: AnyNode, node: XdmNode): this(parent, parent.stepConfig, node)

    var conditional: String? = null
    val attributes: MutableMap<QName, String> = mutableMapOf()

    init {
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            attributes[attr.nodeName] = attr.stringValue
        }
        if (node.nodeName.namespaceUri == NsP.namespace) {
            conditional = node.getAttributeValue(Ns.useWhen)
            attributes.remove(Ns.useWhen)
        } else {
            conditional = node.getAttributeValue(NsP.useWhen)
            attributes.remove(NsP.useWhen)
        }
        if (conditional == null) {
            useWhen = true
        } else {
            useWhen = null
        }
    }

    internal open fun computeUseWhenOnThisElement(context: UseWhenContext) {
        if (useWhen == null) {
            context.useWhen.add(this)
            val resolved = context.resolveUseWhen(stepConfig, conditional!!)
            if (resolved != null) {
                useWhen = resolved
                context.useWhen.remove(this)
                context.resolvedCount++
            }
        }
    }

    internal open fun resolveUseWhen(context: UseWhenContext) {
        if (useWhen == false) {
            return
        }

        computeUseWhenOnThisElement(context)

        if (useWhen == true) {
            var inline = false
            var tnode: AnyNode? = this
            while (!inline && tnode != null) {
                inline = tnode.node.nodeName == NsP.inline
                tnode = tnode.parent
            }

            for (child in children.filterIsInstance<ElementNode>()) {
                child.resolveUseWhen(context)
            }
        }
    }

    override fun toString(): String {
        return node.nodeName.toString()
    }
}