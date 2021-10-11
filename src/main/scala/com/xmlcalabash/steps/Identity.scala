package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}

class Identity() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    //println(s"IDENTITY $item")
    consumer.get.receive("result", item, metadata)
  }
}
