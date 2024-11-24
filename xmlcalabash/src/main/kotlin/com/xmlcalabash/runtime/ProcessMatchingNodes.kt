package com.xmlcalabash.runtime

import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.XdmNode

interface ProcessMatchingNodes {
    fun startDocument(node: XdmNode): Boolean
    fun endDocument(node: XdmNode)

    fun startElement(node: XdmNode, attributes: AttributeMap): Boolean
    fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap?
    fun endElement(node: XdmNode)

    fun text(node: XdmNode)
    fun comment(node: XdmNode)
    fun pi(node: XdmNode)
}