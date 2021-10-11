package com.xmlcalabash.steps.internal

import java.net.{URI, URLConnection}
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.DocumentLoaderParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, XdmMap, XdmNode, XdmValue}

import scala.collection.mutable.ListBuffer

// N.B. This looks like a step, but it isn't really. It gets passed all of the variable bindings
// and the context item and it evaluates its "options" directly. This is necessary because in
// the case where this is a default binding, it must *not* evaluate its options if the default
// is not used.

class DocumentLoader() extends AbstractLoader {
  private var _hrefAvt = List.empty[String]
  private var _parameters = Option.empty[String]
  private var params = Map.empty[QName, XdmValue]
  private var contextProvided = false
  private var baseURI = Option.empty[URI]

  override def inputSpec: XmlPortSpecification = {
    if (contextProvided) {
      XmlPortSpecification.ANYSOURCESEQ
    } else {
      XmlPortSpecification.NONE
    }
  }
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    super.configure(config, stepType, stepName, params)

    if (params.isEmpty) {
      throw XProcException.xiThisCantHappen("Document loader called without params", location)
    }

    params.get match {
      case doc: DocumentLoaderParams =>
        _hrefAvt = doc.hrefAvt
        _document_properties = doc.document_properties
        _parameters = doc.parameters
        content_type = doc.content_type
        contextProvided = doc.context_provided
        baseURI = doc.context.baseURI
        exprContext = doc.context
      case _ =>
        throw new RuntimeException("document loader params wrong type")
    }
  }

  override def runningMessage(): Unit = {
    // nop, we do it after we've computed the href attribute
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val parts = ListBuffer.empty[String]
    var exprseg = false
    for (item <- _hrefAvt) {
      if (exprseg) {
        val expr = new XProcXPathExpression(exprContext, item)
        val value = xpathValue(expr)
        parts += value.getUnderlyingValue.getStringValue
      } else {
        parts += item
      }
      exprseg = !exprseg
    }

    val href = if (baseURI.isDefined) {
      baseURI.get.resolve(parts.mkString(""))
    } else {
      new URI(parts.mkString(""))
    }

    if (DefaultXmlStep.showRunningMessage) {
      logger.info("Loading document: {}", href)
    }

    if (_parameters.isDefined) {
      val expr = new XProcXPathExpression(exprContext, _parameters.get)
      val result = xpathValue(expr)
      params = result match {
        case map: XdmMap =>
          ValueParser.parseParameters(map, context)
        case _ =>
          throw XProcException.xsBadTypeValue("parameters", "map", location)
      }
    }

    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. See
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8039362
    val contentTypeString = Option(URLConnection.guessContentTypeFromName(href.toASCIIString)).getOrElse("application/xml")

    val propContentType = if (docProps.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(docProps.get(XProcConstants._content_type).toString))
    } else {
      None
    }

    val contentType = if (propContentType.isDefined) {
      if (content_type.isDefined) {
        if (!content_type.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(content_type.get, propContentType.get, location)
        }
      }
      propContentType.get
    } else {
      if (content_type.isDefined) {
        content_type.get
      } else {
        MediaType.parse(contentTypeString)
      }
    }

    val request = new DocumentRequest(Some(href), Some(contentType), location, params)
    request.docprops = docProps

    val result = config.documentManager.parse(request)
    val metadata = new XProcMetadata(result.contentType, docProps ++ result.props)

    if (result.shadow.isDefined) {
      val binary = new BinaryNode(config, result.shadow.get)
      consumer.get.receive("result", binary, metadata)
    } else {
      result.value match {
        case node: XdmNode =>
          val patched = S9Api.patchBaseURI(config.config, node, metadata.baseURI)
          consumer.get.receive("result", patched, metadata)
        case _ =>
          consumer.get.receive("result", result.value, metadata)
      }
    }
  }
}
