package com.xmlcalabash.steps.text

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import scala.collection.mutable.ListBuffer

class Tail() extends TextLines {
  private val _count = new QName("", "count")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val count = integerBinding(_count).get
    var newLines = ListBuffer.empty[String]

    if (count == 0) {
      newLines = lines
    } else if (count > 0) {
      if (count < lines.size) {
        newLines = lines.drop(lines.size - count)
      } else {
        newLines = lines
      }
    } else if (count < 0) {
      newLines = lines.dropRight(-count)
    }

    val hlines = new StringBuilder()
    for (line <- newLines) {
      hlines.append(line)
      hlines.append("\n")
    }

    consumer.get.receive("result", new XdmAtomicValue(hlines.toString), new XProcMetadata(MediaType.TEXT))
  }
}
