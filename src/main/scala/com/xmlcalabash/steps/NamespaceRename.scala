package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.`type`.{BuiltInAtomicType, Untyped}
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, FingerprintedQName, NameOfNode, NamespaceMap, NodeInfo}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
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

    if (from == to) {
      consumer.receive("result", source, metadata)
    } else {
      matcher = new ProcessMatch(config, this, context)
      matcher.process(source, "*")
      consumer.receive("result", matcher.result, metadata)
    }
  }

  override def startDocument(node: XdmNode): Boolean = true

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    val nshash = mutable.HashMap.empty[String,String]
    nshash.put(node.getNodeName.getPrefix, node.getNodeName.getNamespaceURI)

    val elemPrefix = node.getNodeName.getPrefix
    val appliesToElement = node.getNodeName.getNamespaceURI == from
    if (!appliesToElement && applyTo == "elements") {
      matcher.addStartElement(node, attributes)
      return true
    }

    val inode = node.getUnderlyingNode

    var attrPrefix = ""
    for (ns <- inode.getAllNamespaces.iterator().asScala) {
      nshash.put(ns.getPrefix, ns.getURI)
      if (ns.getURI == from) {
        if (attrPrefix == "" || ns.getPrefix == elemPrefix) {
          attrPrefix = ns.getPrefix
        }
      }
    }

    var hasAttr = false
    for (ainfo <- attributes.asList().asScala) {
      hasAttr = hasAttr || (ainfo.getNodeName.getURI == from)
    }

    var newPrefix = if (appliesToElement) {
      elemPrefix
    } else {
      attrPrefix
    }

    if (to == "") {
      newPrefix = ""
    }

    if (elemPrefix == attrPrefix && hasAttr && from != "" && applyTo != "all") {
      var count = 1
      newPrefix = s"_${count}"
      while (nshash.contains(newPrefix)) {
        count += 1
        newPrefix = s"_${count}"
      }
    }

    var newNS = NamespaceMap.emptyMap()
    newNS = newNS.put(newPrefix, to)

    var startName = NameOfNode.makeName(inode)
    var startType = inode.getSchemaType
    var startAttr = inode.attributes()

    if (applyTo != "attributes" && appliesToElement) {
      startName = new FingerprintedQName(newPrefix, to, node.getNodeName.getLocalName)
      startType = Untyped.INSTANCE
    }

    var forceAttrPrefix = Option.empty[String]
    if (applyTo == "attributes" && appliesToElement) {
      forceAttrPrefix = Some(newPrefix)
      newNS = newNS.put(elemPrefix, from)
    }

    applyTo match {
      case "all" =>
        startAttr = patchAttributes(inode, forceAttrPrefix)

      case "elements" =>
        ()

      case "attributes" =>
        startAttr = patchAttributes(inode, forceAttrPrefix)
    }

    newNS = attributeNamespaceMap(newNS, startAttr)
    matcher.addStartElement(startName, startAttr, startType, newNS)
    true
  }

  private def patchAttributes(inode: NodeInfo, forceAttrPrefix: Option[String]): AttributeMap = {
    var startAttr = inode.attributes()

    for (attr <- inode.attributes().asScala) {
      var nameCode = attr.getNodeName
      var atype = attr.getType

      if (from == nameCode.getURI) {
        startAttr = startAttr.remove(nameCode)
        val pfx = forceAttrPrefix.getOrElse(nameCode.getPrefix)
        nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart)
        if (startAttr.get(nameCode) != null) {
          throw XProcException.xcAttributeNameCollision(nameCode.getLocalPart, location)
        }
        atype = BuiltInAtomicType.UNTYPED_ATOMIC
      }

      startAttr = startAttr.put(new AttributeInfo(nameCode, atype, attr.getValue, attr.getLocation, ReceiverOption.NONE))
    }

    startAttr
  }

  private def attributeNamespaceMap(initialNSMap: NamespaceMap, attrmap: AttributeMap): NamespaceMap = {
    var nsMap = initialNSMap
    for (attr <- attrmap.asList().asScala) {
      val pfx = attr.getNodeName.getPrefix
      val ns = attr.getNodeName.getURI
      if (pfx != "") {
        nsMap = nsMap.put(pfx, ns)
      }
    }
    nsMap
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
