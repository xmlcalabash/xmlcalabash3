package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode, XdmNodeKind}

import scala.collection.mutable.ListBuffer

class Replace() extends DefaultXmlStep  with ProcessMatchingNodes {
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var replacement: ListBuffer[XdmNode] = ListBuffer.empty[XdmNode]
  private var pattern: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE, "replacement" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/plain"), "replacement" -> List("text", "xml", "html"))
  )
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item.asInstanceOf[XdmNode]
      this.metadata = metadata
    } else {
      for (node <- S9Api.axis(item.asInstanceOf[XdmNode], Axis.CHILD)) {
        replacement += node
      }
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    pattern = stringBinding(XProcConstants._match)

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    if (documentIsText(result)) {
      metadata = convertMetadataToText(metadata)
    }

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def reset(): Unit = {
    super.reset()
    replacement.clear()
  }

  override def startDocument(node: XdmNode): Boolean = {
    matcher.startDocument(node.getBaseURI)
    doReplace()
    false
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    doReplace()
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    doReplace()
  }

  override def comment(node: XdmNode): Unit = {
    doReplace()
  }

  override def pi(node: XdmNode): Unit = {
    doReplace()
  }

  def doReplace(): Unit = {
    for (node <- replacement) {
      matcher.addSubtree(node)
    }
  }
}
