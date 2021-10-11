package com.xmlcalabash.steps

import java.io.{IOException, StringReader}

import com.jafpl.graph.Location
import com.jafpl.steps.PortCardinality
import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.auto.AutoSchemaReader
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.thaiopensource.validate.{SchemaReader, ValidateProperty, ValidationDriver}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcLocation, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.xc.Errors
import com.xmlcalabash.util.{CachingErrorListener, S9Api}
import net.sf.saxon.s9api.{QName, XdmNode}
import org.xml.sax.InputSource

import scala.xml.{SAXException, SAXParseException}

class ValidateWithRNG() extends DefaultXmlStep {
  private val _assert_valid = new QName("", "assert-valid")
  private val _dtd_attribute_values = new QName("", "dtd-attribute-values")
  private val _dtd_id_idref_warnings = new QName("", "dtd-id-idref-warnings")
  private val language = "http://relaxng.org/ns/structure/1.0"

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private var schema: XdmNode = _
  private var schemaMetadata: XProcMetadata = _
  private var assert_valid = true
  private var dtd_attribute_values = false
  private var dtd_id_idref_warnings = false

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml", "text/plain")))

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
      "report" -> PortCardinality.ZERO_OR_MORE),
    Map("result" -> List("application/xml", "text/xml", "*/*+xml"),
      "report" -> List("application/xml", "text/xml", "*/*+xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        port match {
          case "source" =>
            source = node
            sourceMetadata = metadata
          case "schema" =>
            schema = node
            schemaMetadata = metadata
          case _ => logger.debug(s"Unexpected connection to p:validate-with-relax-ng: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to xsd validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    if (definedBinding(_dtd_id_idref_warnings)) {
      dtd_id_idref_warnings = booleanBinding(_dtd_id_idref_warnings).getOrElse(false)
    }

    assert_valid = booleanBinding(_assert_valid).getOrElse(true)

    val report = new Errors(config.config)
    val listener = new CachingErrorListener(report)
    val properties = new PropertyMapBuilder()
    properties.put(ValidateProperty.ERROR_HANDLER, listener)
    properties.put(ValidateProperty.URI_RESOLVER, config.uriResolver)
    properties.put(ValidateProperty.ENTITY_RESOLVER, config.entityResolver)

    if (dtd_id_idref_warnings) {
      RngProperty.CHECK_ID_IDREF.add(properties)
    }

    val compact = schemaMetadata.contentType.textContentType

    val configurer = config.xprocConfigurer.jingConfigurer
    var sr: SchemaReader = null
    var schemaInputSource: InputSource = null

    if (compact) {
      configurer.configRNC(properties)
      sr = CompactSchemaReader.getInstance()

      // Grotesque hack!
      val srdr = new StringReader(schema.getStringValue)
      schemaInputSource = new InputSource(srdr)
      schemaInputSource.setSystemId(schema.getBaseURI.toASCIIString)
    } else {
      configurer.configRNG(properties)
      sr = new AutoSchemaReader()
      schemaInputSource = S9Api.xdmToInputSource(config.config, schema)
    }

    var except: Option[XProcException] = None
    var errors = Option.empty[XdmNode]
    val driver = new ValidationDriver(properties.toPropertyMap, sr)
    if (driver.loadSchema(schemaInputSource)) {
      val din = S9Api.xdmToInputSource(config.config, source)
      if (!driver.validate(din)) {
        if (assert_valid) {
          errors = Some(report.endErrors())
          var errloc: Option[Location] = None
          for (lex <- listener.exceptions) {
            lex match {
              case ex: SAXParseException =>
                if (errloc.isEmpty) {
                  errloc = Some(new XProcLocation(ex))
                }
                if (except.isEmpty) {
                  except = Some(XProcException.xcNotSchemaValidRelaxNG(source.getBaseURI.toASCIIString, lex.getMessage, errloc))
                }
              case _: Exception =>
                if (except.isEmpty) {
                  except = Some(XProcException.xcNotSchemaValidRelaxNG(source.getBaseURI.toASCIIString, lex.getMessage, location))
                }
            }
          }

          if (except.isEmpty) {
            except = Some(XProcException.xcNotSchemaValidRelaxNG(source.getBaseURI.toASCIIString, "RELAX NG validation failed", location))
          }

          except.get.underlyingCauses = listener.exceptions
          except.get.errors = errors.get

          throw except.get
        }
      }
    } else {
      throw XProcException.xcNotSchemaValidRelaxNG(source.getBaseURI.toASCIIString, "Error loading schema", location)
    }

    consumer.get.receive("report", report.endErrors() , XProcMetadata.XML)
    consumer.get.receive("result", source, sourceMetadata)
  }
}
