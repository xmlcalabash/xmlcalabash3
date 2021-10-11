package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, FingerprintedQName}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode}
import net.sf.saxon.value.QNameValue

import scala.jdk.CollectionConverters.ListHasAsScala

class Rename() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _new_name = new QName("new-name")

  private var context: StaticContext = _
  private var newName: QName = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    newName = qnameBinding(_new_name).get
    pattern = stringBinding(XProcConstants._match)
    this.context = context

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    matcher.addStartElement(newName, node.getUnderlyingNode.attributes())
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    if (matchingAttributes.size() > 1) {
      // FIXME: should be an xprocexception
      throw new RuntimeException("Cannot rename multiple attributes to the same name")
    }

    var amap = nonMatchingAttributes
    var nsmap = node.getUnderlyingNode.getAllNamespaces
    for (attr <- matchingAttributes.asList().asScala) {
      var prefix = newName.getPrefix
      val uri = newName.getNamespaceURI
      val localName = newName.getLocalName

      if (uri == null || uri == "") {
        val fqName = new FingerprintedQName("", "", localName)
        amap = amap.put(new AttributeInfo(fqName, attr.getType, attr.getValue, attr.getLocation, attr.getProperties))
      } else {
        if (prefix == null || prefix == "") {
          prefix = "_"
        }

        var count = 1
        var checkPrefix = prefix
        var nsURI = nsmap.getURI(checkPrefix)
        while (nsURI != null && nsURI != uri) {
          count += 1
          checkPrefix = s"${prefix}${count}"
          nsURI = nsmap.getURI(checkPrefix)
        }

        prefix = checkPrefix
        val fqName = new FingerprintedQName(prefix, uri, localName)
        amap = amap.put(new AttributeInfo(fqName, attr.getType, attr.getValue, attr.getLocation, attr.getProperties))
      }
    }

    Some(amap)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    if (newName.getNamespaceURI != "") {
      throw XProcException.xcBadRenamePI(newName, location)
    }
    matcher.addPI(newName.getLocalName, node.getStringValue)
  }
}
