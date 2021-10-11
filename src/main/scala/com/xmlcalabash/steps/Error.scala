package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{QName, XdmNode}

class Error extends DefaultXmlStep {
  private val _code = new QName("", "code")

  private var _message = Option.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => _message = Some(node)
      case _ => logger.debug(s"p:error received unexpected item: $item")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val code = qnameBinding(_code).get

    val tree = new SaxonTreeBuilder(config)
    if (_message.isDefined) {
      tree.startDocument(_message.get.getBaseURI)
    } else {
      tree.startDocument(context.baseURI)
    }

    tree.addStartElement(XProcConstants.c_errors)

    var nsmap = NamespaceMap.emptyMap()
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, code.toString))
    if (context.location.isDefined) {
      if (context.location.get.uri.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, context.location.get.uri.get))
      }
      if (context.location.get.line.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._line, context.location.get.line.get.toString))
      }
      if (context.location.get.column.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._column, context.location.get.column.get.toString))
      }
    }
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._type, "p:error"))
    nsmap = nsmap.put("p", XProcConstants.ns_p)
    tree.addStartElement(XProcConstants.c_error, amap, nsmap)
    if (_message.isDefined) {
      tree.addSubtree(_message.get)
    }
    tree.addEndElement()
    tree.addEndElement()
    tree.endDocument()

    throw XProcException.xcGeneralException(code, Some(tree.result), context.location)
  }
}
