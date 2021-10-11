package com.xmlcalabash.util

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.jdk.CollectionConverters._

class StepErrors(config: XMLCalabashRuntime) {
  private val _code = new QName("", "code")

  def error(code: QName): XdmNode = {
    error(code, None)
  }

  def error(code: QName, message: String): XdmNode = {
    error(code, Some(message))
  }

  def error(code: QName, message: Option[String]): XdmNode = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(config.staticBaseURI)
    builder.addStartElement(XProcConstants.c_errors)

    var nsmap = NamespaceMap.emptyMap()
    nsmap = nsmap.put(code.getPrefix, code.getNamespaceURI)

    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_code, code.toString))

    builder.addStartElement(XProcConstants.c_error, amap, nsmap)

    if (message.isDefined) {
      builder.addStartElement(XProcConstants.c_message)
      builder.addText(message.get)
      builder.addEndElement()
    }

    builder.addEndElement()
    builder.addEndElement()
    builder.endDocument()
    builder.result
  }
}
