package com.xmlcalabash.steps.internal

import com.jafpl.messages.Message
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.InlineLoaderParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XProcMetadata, XProcVtExpression, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}

import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Base64
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

// N.B. This looks like a step, but it isn't really. It gets passed all of the variable bindings
// and the context item and it evaluates its "options" directly. This is necessary because in
// the case where this is a default binding, it must *not* evaluate its options if the default
// is not used.

class InlineLoader() extends AbstractLoader {
  private var node: XdmNode = _
  private var encoding = Option.empty[String]
  private var exclude_inline_prefixes = Option.empty[String]
  private var expandText = false
  private var contextProvided = false

  override def inputSpec: XmlPortSpecification = {
    if (contextProvided) {
      XmlPortSpecification.ANYSOURCESEQ
    } else {
      XmlPortSpecification.NONE
    }
  }
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw new RuntimeException("inline loader params required")
    }

    params.get match {
      case doc: InlineLoaderParams =>
        node = doc.document
        content_type = doc.content_type
        encoding = doc.encoding
        _document_properties = doc.document_properties
        exclude_inline_prefixes = doc.exclude_inline_prefixes
        expandText = doc.expand_text
        contextProvided = doc.context_provided
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

    val propContentType = if (docProps.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(docProps.get(XProcConstants._content_type).toString))
    } else {
      None
    }

    contentType = if (propContentType.isDefined) {
      if (content_type.isDefined) {
        if (!content_type.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(content_type.get, propContentType.get, exprContext.location)
        }
      }
      propContentType.get
    } else {
      if (content_type.isDefined) {
        content_type.get
      } else {
        MediaType.XML
      }
    }

    if (DefaultXmlStep.showRunningMessage) {
      val root = S9Api.documentElement(node)
      val show = if (root.isDefined) {
        s"<${root.get.getNodeName}>"
      } else {
        var text = node.getStringValue.replace("\n", " ")
        text = text.replaceAll("\\s+", " ")
        text = text.replaceAll("^\\s", "")
        if (text.length > 25) {
          text = text.substring(0, 25) + "..."
        }
        text
      }
      logger.info("Loading inline {}: {}", contentType, show)
    }

    val meta = new XProcMetadata(contentType, docProps)

    if (meta.baseURI.isDefined) {
      // It must have come from document properties; try to patch the node
      if (contentType.markupContentType) {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(meta.baseURI.get)
        builder.addSubtree(node)
        builder.endDocument()
        node = builder.result
      }
    }

    val expander = new InlineExpander(config.config, node, meta, exprContext, location)
    expander.contentType = contentType
    expander.encoding = encoding
    expander.excludeURIs = if (exclude_inline_prefixes.isDefined) {
      S9Api.urisForPrefixes(node, exclude_inline_prefixes.get.split("\\s+").toSet)
    } else {
      Set()
    }
    expander.msgBindings = msgBindings.toMap
    expander.expandText = expandText
    expander.contextItem = contextItem
    expander.documentProperties = docProps

    val req = expander.loadDocument()
    val resp = config.documentManager.parse(req)
    if (resp.shadow.isDefined) {
      consumer.get.receive("result", resp.shadow.get, new XProcMetadata(resp.contentType, resp.props))
    } else {
      consumer.get.receive("result", resp.value, new XProcMetadata(resp.contentType, resp.props))
    }
  }
}
