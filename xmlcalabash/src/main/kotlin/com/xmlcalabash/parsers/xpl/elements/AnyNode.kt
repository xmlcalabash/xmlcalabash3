package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.XdmNode
import java.net.URI

open class AnyNode(val parent: AnyNode?, parentConfig: InstructionConfiguration, val node: XdmNode) {
    constructor(parent: AnyNode, node: XdmNode): this(parent, parent.stepConfig, node)

    internal val stepConfig = if (node.nodeName == NsP.declareStep || node.nodeName == NsP.library) {
        parentConfig.copyNew()
    } else {
        parentConfig.copy()
    }

    val baseUri: URI
    val children = mutableListOf<AnyNode>()
    var useWhen: Boolean? = true

    init {
        stepConfig.updateWith(node)
        baseUri = node.baseURI
    }

    override fun toString(): String {
        return node.nodeKind.toString()
    }
}