package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, URIUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}

class Count() extends DefaultXmlStep {
  private val _limit = new QName("", "limit")

  private var count = 0L
  private var limit = 0L
  private var sent = false

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def reset(): Unit = {
    super.reset()
    count = 0
    sent = false
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      if (limit <= 0 || count != limit) {
        count += 1
      }
    }
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == _limit) {
      limit = variable.value.asInstanceOf[XdmAtomicValue].getLongValue
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    sendCount()
  }

  private def sendCount(): Unit = {
    if (sent) {
      return
    } else {
      sent = true
    }

    val value = if ((limit == 0) || (count <= limit)) {
      count
    } else {
      limit
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URIUtils.cwdAsURI)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(value.toString)
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
