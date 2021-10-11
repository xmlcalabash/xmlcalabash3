package com.xmlcalabash.steps

import com.nwalsh.sinclude.exceptions.{MalformedXPointerSchemeException, UnknownXPointerSchemeException, UnparseableXPointerSchemeException, XIncludeFallbackException, XIncludeIOException}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode}
import net.sf.saxon.trans.XPathException

import scala.xml.include.XIncludeException

class XInclude() extends DefaultXmlStep {
  private val _fixup_xml_base = new QName("fixup-xml-base")
  private val _fixup_xml_lang = new QName("fixup-xml-lang")
  private val cx_trim = new QName("cx", XProcConstants.ns_cx, "trim")

  private var source: XdmNode = _
  private var smeta: XProcMetadata = _

  private var staticContext: StaticContext = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.MARKUPSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    smeta = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    staticContext = context
    super.run(context)

    val fixupBase = booleanBinding(_fixup_xml_base).getOrElse(false)
    val fixupLang = booleanBinding(_fixup_xml_lang).getOrElse(false)
    val copyAttributes = true // XInclude 1.1

    val trimText = booleanBinding(cx_trim).getOrElse(false)

    val xincluder = new com.nwalsh.sinclude.XInclude()
    xincluder.setTrimText(trimText)
    xincluder.setFixupXmlBase(fixupBase)
    xincluder.setCopyAttributes(copyAttributes)
    xincluder.setFixupXmlLang(fixupLang)

    try {
      consumer.get.receive("result", xincluder.expandXIncludes(source), smeta)
    } catch {
      case ex: XIncludeIOException =>
        throw XProcException.xcXIncludeError(ex.getMessage, location)
      case ex: XIncludeException =>
        throw XProcException.xcXIncludeError(ex.getMessage, location)
      case ex: Throwable =>
        throw XProcException.xcXIncludeError(ex.getMessage, location)
    }
  }
}
