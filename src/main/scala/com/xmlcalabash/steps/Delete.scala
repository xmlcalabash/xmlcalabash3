package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.XdmNode

class Delete() extends DefaultXmlStep  with ProcessMatchingNodes {
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    pattern = stringBinding(XProcConstants._match)

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, source_metadata))
    }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    Some(nonMatchingAttributes)
  }

  override def text(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def comment(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def pi(node: XdmNode): Unit = {
    // nop, deleted
  }
}
