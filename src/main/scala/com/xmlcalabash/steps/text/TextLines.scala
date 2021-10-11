package com.xmlcalabash.steps.text

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class TextLines() extends DefaultXmlStep {
  private var text: XdmNode = _
  private var meta: XProcMetadata = _
  protected val lines: ListBuffer[String] = ListBuffer.empty[String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        text = node
        var svalue = text.getStringValue
        if (svalue != "") {
          // I'm a little dubious about this, but see test ab-text-count-004
          svalue = svalue.replaceAll("\r([^\n])", "\n$1")
          lines ++= svalue.split('\n')
          // String.split() doesn't include trailing empty lines, but we need them.
          var pos = svalue.length - 1
          if (svalue.charAt(pos) == '\n') {
            pos -= 1
          }
          while (pos >= 0 && svalue.charAt(pos) == '\n') {
            lines += ""
            pos -= 1
          }
        }
      case _ =>
        throw XProcException.xiUnexpectedItem(item.toString, location)
    }
    meta = metadata
  }
}
