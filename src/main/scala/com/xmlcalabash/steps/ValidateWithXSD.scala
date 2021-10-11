package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.xc.Errors
import com.xmlcalabash.util.{CachingErrorListener, MediaType, S9Api}
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.Controller
import net.sf.saxon.`type`.ValidationException
import net.sf.saxon.s9api.{QName, SaxonApiException, SchemaManager, XdmDestination, XdmNode}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.mutable.ListBuffer

class ValidateWithXSD() extends DefaultXmlStep {
  private val _use_location_hints = new QName("","use-location-hints")
  private val _try_namespaces = new QName("", "try-namespaces")
  private val _assert_valid = new QName("", "assert-valid")
  private val _mode = new QName("", "mode")
  private val _version = new QName("", "version")
  private val _targetNamespace = new QName("", "targetNamespace")

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private val schemas = ListBuffer.empty[XdmNode]
  private var use_location_hints = false
  private var try_namespaces = false
  private var assert_valid = true
  private var mode = "strict"
  private var version = "1.1"

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml")))

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
          case "schema" => schemas += node
          case _ => logger.debug(s"Unexpected connection to p:validate-with-xsd: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to xsd validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val manager = Option(config.processor.getSchemaManager)
    if (manager.isDefined) {
      validateWithSaxon(manager.get)
    } else {
      throw new RuntimeException("Validation requires Saxon EE")
    }
  }

  private def validateWithSaxon(manager: SchemaManager): Unit = {
    logger.trace(s"Validating with Saxon: ${source.getBaseURI} with ${schemas.length} schema(s)")

    version = stringBinding(_version, version)
    if (version != "1.0" && version != "1.1") {
      throw XProcException.xcVersionNotAvailable(version, location)
    }

    val saxonConfig = config.processor.getUnderlyingConfiguration
    saxonConfig.clearSchemaCache()

    try_namespaces = booleanBinding(_try_namespaces).getOrElse(false)
    mode = stringBinding(_mode, mode)
    assert_valid = booleanBinding(_assert_valid).getOrElse(true)
    use_location_hints = booleanBinding(_use_location_hints).getOrElse(false)

    // Populate the URI cache so that URI references in schema documents will
    // preferentially find the schemas provided
    val schemaDocuments = ListBuffer.empty[XdmNode]
    for (schema <- schemas) {
      val schemaNode = S9Api.documentElement(schema)
      val targetNS = if (schemaNode.isDefined) {
        Option(schemaNode.get.getAttributeValue(_targetNamespace)).getOrElse("")
      } else {
        ""
      }
      logger.debug(s"Caching input schema: ${schema.getBaseURI} for $targetNS")
      // FIXME: populate the runtime resolver cache with these documents!
      schemaDocuments += schema
    }

    val sourceNode = S9Api.documentElement(source)
    if (use_location_hints && sourceNode.isDefined) {
      val nonsSchemaHint = Option(sourceNode.get.getAttributeValue(XProcConstants.xsi_noNamespaceSchemaLocation))
      val schemaHint = Option(sourceNode.get.getAttributeValue(XProcConstants.xsi_schemaLocation))
      if (nonsSchemaHint.isDefined) {
        val uri = sourceNode.get.getBaseURI.resolve(nonsSchemaHint.get)
        val resp = config.documentManager.parse(new DocumentRequest(uri, MediaType.XML))
        schemaDocuments += resp.value.asInstanceOf[XdmNode]
      }
      if (schemaHint.isDefined) {
        val parts = schemaHint.get.split("\\s+")
        var idx = 1
        while (idx < parts.length) {
          val uri = sourceNode.get.getBaseURI.resolve(parts(idx))
          val resp = config.documentManager.parse(new DocumentRequest(uri, MediaType.XML))
          schemaDocuments += resp.value.asInstanceOf[XdmNode]
          idx += 2
        }
      }
    }

    if (try_namespaces && sourceNode.isDefined) {
      val ns = sourceNode.get.getNodeName.getNamespaceURI
      if (ns != "") {
        val resp = config.documentManager.parse(new DocumentRequest(new URI(ns), MediaType.XML))
        schemaDocuments += resp.value.asInstanceOf[XdmNode]
      }
    }

    for (schema <- schemaDocuments) {
      val schemaSource = S9Api.xdmToInputSource(config.config, schema)
      schemaSource.setSystemId(schema.getBaseURI.toASCIIString)
      val source = new SAXSource(schemaSource)
      manager.load(source)
    }

    val destination = new XdmDestination
    val controller = new Controller(saxonConfig)
    val pipe = controller.makePipelineConfiguration()
    val receiver = destination.getReceiver(pipe, new SerializationProperties())
    pipe.setRecoverFromValidationErrors(assert_valid)
    receiver.setPipelineConfiguration(pipe)

    val report = new Errors(config.config)
    val listener = new CachingErrorListener(report)
    val validator = manager.newSchemaValidator()
    validator.setDestination(destination)
    validator.setErrorListener(listener)
    validator.setLax(mode == "lax")
    validator.setUseXsiSchemaLocation(use_location_hints)

    var raisedException = Option.empty[Exception]
    var errors = Option.empty[XdmNode]

    try {
      validator.validate(source.asSource())
    } catch {
      case ex: SaxonApiException =>
        errors = Some(report.endErrors())
        var msg = ex.getMessage
        if (listener.exceptions.nonEmpty) {
          val lex = listener.exceptions.head
          lex match {
            case ve: ValidationException =>
              msg = ve.getMessage
              val fail = ve.getValidationFailure
              val except = XProcException.xcNotSchemaValidXmlSchema(source.getBaseURI.toASCIIString, fail.getLineNumber, fail.getColumnNumber, msg, location)
              except.underlyingCauses = listener.exceptions
              except.errors = errors.get
              raisedException = Some(except)
            case _: Exception =>
              msg = lex.getMessage
              val except = XProcException.xcNotSchemaValidXmlSchema(source.getBaseURI.toASCIIString, msg, location)
              except.underlyingCauses = listener.exceptions
              except.errors = errors.get
              raisedException = Some(except)
          }
        } else {
          val except = XProcException.xcNotSchemaValidXmlSchema(source.getBaseURI.toASCIIString, msg, location)
          raisedException = Some(except)
        }
      case ex: Exception =>
        errors = Some(report.endErrors())
        val except = XProcException.xcNotSchemaValidXmlSchema(source.getBaseURI.toASCIIString, ex.getMessage, location)
        except.underlyingCauses = listener.exceptions
        except.errors = errors.get
        raisedException = Some(except)
    }

    if (raisedException.isDefined) {
      if (assert_valid) {
        throw raisedException.get
      } else {
        consumer.get.receive("report", errors.get, XProcMetadata.XML)
        consumer.get.receive("result", source, sourceMetadata)
      }
    } else {
      errors = Some(report.endErrors())
      consumer.get.receive("report", errors.get, XProcMetadata.XML)
      consumer.get.receive("result", destination.getXdmNode, sourceMetadata)
    }
  }
}
