package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{HashUtils, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.jdk.CollectionConverters.ListHasAsScala

class Uuid() extends DefaultXmlStep  with ProcessMatchingNodes {
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var uuid: String = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    this.metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val version = integerBinding(XProcConstants._version)
    if (version.isEmpty || version.get == 4) {
      val id = java.util.UUID.randomUUID
      uuid = id.toString
    } else {
      throw XProcException.xcUnsupportedUuidVersion(version.get, location)
    }

    pattern = stringBinding(XProcConstants._match)
    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, metadata))
  }

  override def startDocument(node: XdmNode): Boolean = {
    matcher.addText(uuid)
    false
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    matcher.addText(uuid)
    false
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    for (attr <- nonMatchingAttributes.asList().asScala) {
      amap = amap.put(attr)
    }
    for (attr <- matchingAttributes.asList().asScala) {
      amap = amap.put(new AttributeInfo(attr.getNodeName, BuiltInAtomicType.UNTYPED_ATOMIC, uuid, attr.getLocation, ReceiverOption.NONE))
    }
    Some(amap)
  }

  override def endElement(node: XdmNode): Unit = {
    // nop
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }
}
