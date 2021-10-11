package com.xmlcalabash.runtime

import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.XdmNode

trait ProcessMatchingNodes {
  def startDocument(node: XdmNode): Boolean
  def endDocument(node: XdmNode): Unit

  def startElement(node: XdmNode, attribute: AttributeMap): Boolean
  def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap]
  def endElement(node: XdmNode): Unit

  def text(node: XdmNode): Unit
  def comment(node: XdmNode): Unit
  def pi(node: XdmNode): Unit
}
