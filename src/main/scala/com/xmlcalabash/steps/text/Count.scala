package com.xmlcalabash.steps.text

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType

class Count() extends TextLines {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(lines.size.toString)
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
