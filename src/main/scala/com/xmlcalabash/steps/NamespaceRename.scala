package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.`type`.{BuiltInAtomicType, Untyped}
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, FingerprintedQName, NameOfNode}
import net.sf.saxon.s9api.{QName, XdmNode}
import scala.jdk.CollectionConverters._

class NamespaceRename() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _from = new QName("from")
  private val _to = new QName("to")
  private val _apply_to = new QName("apply-to")
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var from: String = _
  private var to: String = _
  private var applyTo: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    from = stringBinding(_from)
    to = stringBinding(_to)
    applyTo = stringBinding(_apply_to, "all")

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, "*")

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = true

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    var inode = node.getUnderlyingNode
    val nsmap = inode.getAllNamespaces
    var newNS = inode.getAllNamespaces

    // This code had to be competely rewritten for the Saxon 10 API

    var startName = NameOfNode.makeName(inode)
    var startType = inode.getSchemaType
    var startAttr = inode.attributes()

    if (applyTo != "attributes") {
      if (from == node.getNodeName.getNamespaceURI) {
        val prefix = node.getNodeName.getPrefix
        startName = new FingerprintedQName(prefix, to, node.getNodeName.getLocalName)
        startType = Untyped.INSTANCE
        newNS = newNS.remove(prefix)
        newNS = newNS.put(prefix, to)
      }
    }

    applyTo match {
      case "all" =>
        for (attr <- inode.attributes().asScala) {
          var nameCode = attr.getNodeName
          var atype = attr.getType

          if (from == nameCode.getURI) {
            startAttr = startAttr.remove(nameCode)
            val pfx = nameCode.getPrefix
            newNS = newNS.remove(pfx)

            nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart)
            atype = BuiltInAtomicType.UNTYPED_ATOMIC
            newNS = newNS.put(pfx, to)
          }

          startAttr = startAttr.put(new AttributeInfo(nameCode, atype, attr.getValue, attr.getLocation, ReceiverOption.NONE))
        }

      case "elements" =>
        for (attr <- inode.attributes().asScala) {
          var nameCode = attr.getNodeName
          var atype = attr.getType

          if (from == nameCode.getURI) {
            startAttr = startAttr.remove(nameCode)
            val pfx = prefixFor(newNS, from)
            nameCode = new FingerprintedQName(pfx, from, nameCode.getLocalPart)
            atype = BuiltInAtomicType.UNTYPED_ATOMIC
            newNS = newNS.put(pfx, from)
          }

          startAttr = startAttr.put(new AttributeInfo(nameCode, atype, attr.getValue, attr.getLocation, ReceiverOption.NONE))
        }

      case "attributes" =>
        for (attr <- inode.attributes().asScala) {
          var nameCode = attr.getNodeName
          startAttr = startAttr.remove(nameCode)
          var pfx = nameCode.getPrefix
          if (from == nameCode.getURI) {
            pfx = prefixFor(newNS, to)
            nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart)
            newNS = newNS.put(pfx, to)
          }

          startAttr = startAttr.put(new AttributeInfo(nameCode, BuiltInAtomicType.UNTYPED_ATOMIC, attr.getValue, attr.getLocation, ReceiverOption.NONE))
        }
    }

    matcher.addStartElement(startName, startAttr, startType, newNS)
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    throw new UnsupportedOperationException("processAttribute can't be called in NamespaceRename--but it was!?")
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(node.getStringValue)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addComment(node.getStringValue)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addPI(node.getNodeName.getLocalName, node.getStringValue)
  }
}
