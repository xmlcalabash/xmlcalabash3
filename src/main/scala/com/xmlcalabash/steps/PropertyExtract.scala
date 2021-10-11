package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{XdmAtomicValue, XdmNode}

class PropertyExtract extends DefaultXmlStep {
  private var doc = Option.empty[Any]
  private var meta = Option.empty[XProcMetadata]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE, "properties" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*"), "properties" -> List("application/xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    doc = Some(item)
    meta = Some(metadata)
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    consumer.get.receive("result", doc.get, meta.get)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    var nsmap = NamespaceMap.emptyMap()
    nsmap = nsmap.put("xsi", XProcConstants.ns_xsi)
    nsmap = nsmap.put("xs", XProcConstants.ns_xs)

    builder.addStartElement(XProcConstants.c_document_properties, EmptyAttributeMap.getInstance(), nsmap)
    for ((key,value) <- meta.get.properties) {
      var amap: AttributeMap = EmptyAttributeMap.getInstance()

      value match {
        case atomic: XdmAtomicValue =>
          val xtype = atomic.getTypeName
          if (xtype.getNamespaceURI == XProcConstants.ns_xs && (xtype != XProcConstants.xs_string)) {
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants.xsi_type, xtype.toString))
          }
        case _ => ()
      }

      builder.addStartElement(key, amap)
      value match {
        case node: XdmNode =>
          builder.addSubtree(node)
        case atomic: XdmAtomicValue =>
          builder.addValues(value)
        case _ =>
          throw XProcException.xiInvalidPropertyValue(value, location)
      }
      builder.addEndElement()
    }
    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("properties", builder.result, new XProcMetadata(MediaType.XML))
  }
}
