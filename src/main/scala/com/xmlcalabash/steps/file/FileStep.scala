package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.QName

class FileStep() extends DefaultXmlStep {

  protected def errorFromException(builder: SaxonTreeBuilder, exception: Exception): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    var body: String = ""

    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._type, stepType.getClarkName))
    if (stepName.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName.get))
    }

    exception match {
      case xproc: XProcException =>
        val code = xproc.code
        val message = if (xproc.message.isDefined) {
          xproc.message.get
        } else {
          code match {
            case qname: QName =>
              config.config.errorExplanation.message(qname, xproc.variant, xproc.details)
            case _ =>
              s"Configuration error: code ($code) is not a QName"
          }
        }

        code match {
          case qcode: QName =>
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, qcode.getClarkName))
            body = config.config.errorExplanation.explanation(qcode, xproc.variant)
          case _ =>
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, code.toString))
            ()
        }

        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._cause, message))
        if (xproc.location.isDefined) {
          val loc = xproc.location.get
          if (loc.uri.isDefined) {
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, loc.uri.get))
          }
          if (loc.line.isDefined) {
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._line, loc.line.get.toString))
          }
          if (loc.column.isDefined) {
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._column, loc.column.get.toString))
          }
        }

      case _ =>
        body = exception.getMessage
    }

    builder.addStartElement(XProcConstants.c_error, amap)
    builder.addText(body)
    errorDetail(builder)
    builder.addEndElement()
  }

  protected def errorDetail(builder: SaxonTreeBuilder): Unit = {
    // nop
  }
}
