package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.`type`.ValidationException
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.QName
import net.sf.saxon.trans.XPathException
import org.xml.sax.SAXParseException

class ExceptionTranslator() extends DefaultXmlStep {
  private val _uri = new QName("uri")
  private val _line = new QName("line")
  private val _column = new QName("column")
  private val _xpath = new QName("xpath")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    var nsmap = NamespaceMap.emptyMap()
    nsmap = nsmap.put("cx", XProcConstants.ns_cx)

    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(None)
    tree.addStartElement(XProcConstants.c_errors, EmptyAttributeMap.getInstance(), nsmap)

    item match {
      case except: Throwable =>
        formatException(tree, except)
      case _ =>
        throw new RuntimeException(s"Unexpected input to exception translator: $item not an exception!")
    }

    tree.addEndElement()
    tree.endDocument()
    consumer.get.receive("result", tree.result, new XProcMetadata(MediaType.XML))
  }

  private def formatException(tree: SaxonTreeBuilder, exception: Throwable): Unit = {
    exception match {
      case cause: XProcException =>
        formatXProcException(tree, cause)
      case cause: ValidationException =>
        formatValidationException(tree, cause)
      case cause: XPathException =>
        formatXPathException(tree, cause)
      case cause: SAXParseException =>
        formatSAXParseException(tree, cause)
      case _ =>
        formatGenericException(tree, exception)
    }
  }

  private def formatGenericException(tree: SaxonTreeBuilder, exception: Throwable): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants.cx_class, exception.getClass.getName))
    tree.addStartElement(XProcConstants.c_error, amap)
    tree.addText(exception.getMessage)
    tree.addEndElement()
  }

  private def formatXProcException(tree: SaxonTreeBuilder, exception: XProcException): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, exception.code.toString))
    if (exception.location.isDefined) {
      val loc = exception.location.get
      if (loc.uri.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(_uri, loc.uri.get))
      }
      if (loc.line.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(_line, loc.line.get.toString))
      }
      if (loc.column.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(_column, loc.column.get.toString))
      }
    }

    tree.addStartElement(XProcConstants.c_error, amap)
    tree.addText(exception.getMessage)
    tree.addEndElement()

    for (cause <- exception.underlyingCauses) {
      formatException(tree, cause)
    }
  }

  private def formatXPathException(tree: SaxonTreeBuilder, exception: XPathException): Unit = {
    if (exception.getException == null) {
      tree.addStartElement(XProcConstants.c_error)
      tree.addText(exception.getMessage)
      tree.addEndElement()
    } else {
      formatException(tree, exception.getException)
    }
  }

  private def formatSAXParseException(tree: SaxonTreeBuilder, exception: SAXParseException): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_uri, exception.getSystemId))
    amap = amap.put(TypeUtils.attributeInfo(_line, exception.getLineNumber.toString))
    amap = amap.put(TypeUtils.attributeInfo(_line, exception.getLineNumber.toString))

    tree.addStartElement(XProcConstants.c_error, amap)
    tree.addText(exception.getMessage)
    tree.addEndElement()
  }

  private def formatValidationException(tree: SaxonTreeBuilder, exception: ValidationException): Unit = {
    val vf = exception.getValidationFailure

    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, vf.getErrorCode))
    amap = amap.put(TypeUtils.attributeInfo(_uri, vf.getSystemId))
    amap = amap.put(TypeUtils.attributeInfo(_xpath, exception.getPath))
    amap = amap.put(TypeUtils.attributeInfo(_line, vf.getLineNumber.toString))
    amap = amap.put(TypeUtils.attributeInfo(_column, vf.getColumnNumber.toString))

    tree.addStartElement(XProcConstants.c_error, amap)
    tree.addText(vf.getMessage)
    tree.addEndElement()
  }
}
