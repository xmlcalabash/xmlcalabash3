package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable

class Producer() extends DefaultXmlStep {
  private val _items = mutable.ListBuffer.empty[String]

  def items: List[String] = _items.toList
  def items_=(values: List[String]): Unit = {
    _items.clear()
    for (item <- values) {
      _items += item
    }
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    for (item <- items) {
      consumer.get.receive("result", item, new XProcMetadata(MediaType.TEXT))
    }
  }
}
