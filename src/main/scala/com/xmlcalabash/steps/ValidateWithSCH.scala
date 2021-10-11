package com.xmlcalabash.steps

import com.jafpl.graph.Location

import java.io.InputStream
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcLocation, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, SchematronImpl}

import javax.xml.transform.sax.SAXSource
import javax.xml.transform.{Source, URIResolver}
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmDestination, XdmNode}
import org.xml.sax.InputSource

import scala.xml.SAXParseException

class ValidateWithSCH() extends DefaultXmlStep {
  private val _phase = new QName("", "phase")

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private var schema: XdmNode = _
  private var assert_valid = true
  private var phase = Option.empty[String]
  private var format = "svrl"

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml")))

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
        "report" -> PortCardinality.ZERO_OR_MORE),
    Map("result" -> List("application/xml"),
        "report" -> List("application/xml"))
  )

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        port match {
          case "source" =>
            source = node
            sourceMetadata = metadata
          case "schema" => schema = node
          case _ => logger.debug(s"Unexpected connection to p:validate-with-schematron: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to XML Schematron validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    assert_valid = booleanBinding(XProcConstants._assert_valid).getOrElse(assert_valid)
    phase = optionalStringBinding(_phase)
    format = stringBinding(XProcConstants._report_format, format)

    val impl = new SchematronImpl(config)
    // FIXME: handle parameters

    val report = impl.report(source, schema, phase)

    if (assert_valid) {
      val failed = impl.failedAssertions(report)
      if (failed.nonEmpty) {
        val except = XProcException.xcNotSchemaValidSchematron(source.getBaseURI, "Schematron assertions failed", location)
        except.errors = report
        throw except
      }
    }

    consumer.get.receive("report", report, new XProcMetadata(MediaType.XML))
    consumer.get.receive("result", source, sourceMetadata)
  }
}
