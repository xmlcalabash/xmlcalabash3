package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}

class Sink extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // He shoots! He scores! Bit bucket!
  }

}
