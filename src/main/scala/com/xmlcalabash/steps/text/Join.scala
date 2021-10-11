package com.xmlcalabash.steps.text

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode}

import scala.collection.mutable.ListBuffer

class Join() extends DefaultXmlStep {
  private val _separator = new QName("", "separator")
  private val _prefix = new QName("", "prefix")
  private val _suffix = new QName("", "suffix")
  private val docs = ListBuffer.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => docs += node
      case _ =>
        throw XProcException.xiUnexpectedItem(item.toString, location)
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val separator = stringBinding(_separator)

    var result = stringBinding(_prefix)

    var first = true
    for (node <- docs) {
      if (!first) {
        result += separator
      }
      result += node.getStringValue
      first = false
    }

    result += stringBinding(_suffix)

    var contentType = MediaType.TEXT
    if (definedBinding(XProcConstants._override_content_type)) {
      contentType = MediaType.parse(stringBinding(XProcConstants._override_content_type)).assertValid
    }
    if (!contentType.textContentType) {
      throw XProcException.xcContentTypeIsNotText(contentType.toString, location)
    }

    consumer.get.receive("result", new XdmAtomicValue(result), new XProcMetadata(contentType))
  }
}
