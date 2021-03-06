package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XProcItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.model.xxml.{XInline, XMLStaticContext}
import com.xmlcalabash.runtime.{BinaryNode, DynamicContext, XMLCalabashRuntime, XProcExpression, XProcMetadata, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Base64
import scala.collection.mutable
import scala.jdk.CollectionConverters._

// N.B. This looks like a step, but it isn't really. It gets passed all of the variable bindings
// and the context item and it evaluates its "options" directly. This is necessary because in
// the case where this is a default binding, it must *not* evaluate its options if the default
// is not used.

protected[xmlcalabash] class InlineExpander(val config: XMLCalabash, val node: XdmNode, val meta: XProcMetadata, val exprContext: MinimalStaticContext, val location: Option[Location]) {
  private var _documentProperties: Map[QName, XdmValue] = Map.empty[QName, XdmValue]
  private val fq_inline_expand_text = TypeUtils.fqName(XProcConstants._inline_expand_text)
  private val fq_p_inline_expand_text = TypeUtils.fqName(XProcConstants.p_inline_expand_text)
  private val _msgBindings = mutable.HashMap.empty[String, XProcItemMessage]

  var contentType: MediaType = meta.contentType
  var encoding: Option[String] = None
  var excludeURIs: Set[String] = Set()
  var contextItem: Option[XProcItemMessage] = None

  def msgBindings: Map[String, XProcItemMessage] = _msgBindings.toMap
  def msgBindings_=(bindings: Map[String, XProcItemMessage]): Unit = {
    _msgBindings ++= bindings
  }

  def copyStaticOptionsToBindings(runtime: XMLCalabashRuntime): Unit = {
    for ((name,value) <- runtime.staticOptions) {
      if (exprContext.inscopeConstants.contains(name)) {
        _msgBindings.put(name.getClarkName, value)
      }
    }
  }

  def this(inline: XInline) = {
    this(inline.config, inline.content, new XProcMetadata(inline.contentType), inline.staticContext, inline.location)
    encoding = inline.encoding
    excludeURIs = inline.excludeURIs
  }

  def documentProperties: Map[QName, XdmValue] = _documentProperties
  def documentProperties_=(props: String): Unit = {
    val expr = new XProcXPathExpression(exprContext, props)
    val result = xpathValue(expr)
    _documentProperties = result match {
      case map: XdmMap =>
        ValueParser.parseDocumentProperties(map, exprContext, location)
      case _ =>
        throw XProcException.xsBadTypeValue("document-properties", "map", location)
    }
  }
  def documentProperties_=(props: Map[QName, XdmValue]): Unit = {
    _documentProperties = Map.empty[QName, XdmValue] ++ props
  }

  def loadDocument(expandText: Boolean): DocumentRequest = {
    if (encoding.isDefined) {
      if (contentType.xmlContentType || contentType.htmlContentType) {
        throw XProcException.xdCannotEncodeMarkup(encoding.get, contentType, exprContext.location)
      }
      if (encoding.get != "base64") {
        throw XProcException.xsUnsupportedEncoding(encoding.get, exprContext.location)
      }
    }

    val props = mutable.HashMap.empty[QName, XdmValue]
    props ++= _documentProperties
    if (!props.contains(XProcConstants._base_uri)) {
      props.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
    }

    // If it's not an XML content type, make sure it doesn't contain any elements
    if (!contentType.xmlContentType && !contentType.htmlContentType) {
      val iter = node.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        val child = iter.next()
        child.getNodeKind match {
          case XdmNodeKind.TEXT => ()
          case _ =>
            if (encoding.isDefined) {
              throw XProcException.xdNoMarkupAllowedEncoded(child.getNodeName, exprContext.location)
            } else {
              throw XProcException.xdNoMarkupAllowed(child.getNodeName, exprContext.location)
            }
        }
      }
    }

    if (encoding.isDefined) {
      // See https://github.com/xproc/3.0-specification/issues/561
      return dealWithEncodedText(contentType, props)
    }

    if (contentType.xmlContentType || contentType.htmlContentType) {
      val builder = new SaxonTreeBuilder(config)
      builder.startDocument(node.getBaseURI)
      // FIXME: trim whitespace
      expandTVT(node, builder, expandText)
      builder.endDocument()
      val result = builder.result
      val metadata = new XProcMetadata(contentType, props.toMap)
      new DocumentRequest(result, metadata, location)
    } else if (contentType.jsonContentType) {
      val text = if (expandText) {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        expandTVT(node, builder, expandText)
        builder.endDocument()
        val result = builder.result
        result.getStringValue
      } else {
        node.getStringValue
      }

      val expr = new XProcXPathExpression(exprContext, "parse-json($json)")
      val bindingsMap = mutable.HashMap.empty[String, Message]
      val vmsg = new XdmValueItemMessage(new XdmAtomicValue(text), XProcMetadata.JSON, exprContext)
      bindingsMap.put("{}json", vmsg)
      try {
        val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)
        new DocumentRequest(smsg.item, new XProcMetadata(contentType, props.toMap), location)
      } catch {
        case ex: SaxonApiException =>
          if (ex.getMessage.contains("Invalid JSON")) {
            throw XProcException.xdInvalidJson(ex.getMessage, ex, exprContext.location)
          } else {
            throw ex
          }
        case ex: Exception =>
          throw ex
      }
    } else {
      val text = if (expandText) {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        expandTVT(node, builder, expandText)
        builder.endDocument()
        val result = builder.result
        result.getStringValue
      } else {
        node.getStringValue
      }

      if (contentType.textContentType) {
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        new DocumentRequest(result, new XProcMetadata(contentType, props.toMap), location)
      } else {
        // FIXME: what's the right answer for unexpected content types?
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        new DocumentRequest(result, new XProcMetadata(contentType, props.toMap), location)
      }
    }
  }

  private def dealWithEncodedText(contentType: MediaType, props: mutable.HashMap[QName, XdmValue]): DocumentRequest = {
    val str = node.getStringValue
    val decoded = Base64.getMimeDecoder.decode(str)

    val metadata = new XProcMetadata(contentType, props.toMap)

    if (contentType.xmlContentType || contentType.htmlContentType || contentType.textContentType || contentType.jsonContentType) {
      val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), contentType)
      val result = config.documentManager.parse(req, new ByteArrayInputStream(decoded))
      if (result.shadow.isDefined) {
        val binary = new BinaryNode(config, result.shadow)
        new DocumentRequest(binary, metadata, location)
      } else {
        result.value match {
          case node: XdmNode =>
            new DocumentRequest(node, metadata, location)
          case _ =>
            new DocumentRequest(result.value, metadata, location)
        }
      }
    } else {
      // Octet stream, I guess
      props.put(XProcConstants._content_length, new XdmAtomicValue(decoded.length))
      val binary = new BinaryNode(config, decoded)
      new DocumentRequest(binary, new XProcMetadata(contentType, props.toMap), location)
    }
  }

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        var nsmap = NamespaceMap.emptyMap()
        val iter = node.getUnderlyingNode.getAllNamespaces.iterator()
        while (iter.hasNext) {
          val ns = iter.next()
          if (!excludeURIs.contains(ns.getURI)) {
            val prefix = Option(ns.getPrefix).getOrElse("")
            nsmap = nsmap.put(prefix, ns.getURI)
          }
        }

        var newExpand = expandText

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        for (attr <- node.getUnderlyingNode.attributes().asScala) {
          var discardAttribute = false
          if (attr.getNodeName == fq_p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(exprContext.location)
            }
            discardAttribute = true
            newExpand = attr.getValue == "true"
          }
          if (attr.getNodeName == fq_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getValue == "true"
            }
          }
          if (!discardAttribute) {
            if (expandText) {
              amap = amap.put(new AttributeInfo(attr.getNodeName, BuiltInAtomicType.UNTYPED_ATOMIC, expandString(attr.getValue), null, ReceiverOption.NONE))
            } else {
              amap = amap.put(attr)
            }
          }
        }

        builder.addStartElement(node.getNodeName, amap, nsmap)
        val citer = node.axisIterator(Axis.CHILD)
        while (citer.hasNext) {
          val child = citer.next()
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        val str = node.getStringValue
        if (expandText && str.contains("{")) {
          expandNodes(str, builder)
        } else {
          builder.addText(str.replace("}}", "}"))
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String): String = {
    val vtexpr = new XProcVtExpression(exprContext, text)

    // This code used to just evaluate the template expression and treat the
    // static and dynamic parts as part of the same result. But that doesn't
    // work because we need to put a space between sequences that are
    // the results of evaluating the expression, but not between the static
    // and dynamic parts.
    //
    // "test-{1+1}" => "test-2"
    // "{(3,4,5)}" => "3 4 5"

    val sb = new StringBuffer()

    var template = false
    for (part <- vtexpr.avt) {
      if (template) {
        val expr = new XProcXPathExpression(exprContext, part)
        val evaluator = config.expressionEvaluator.newInstance()
        val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
        var s = ""
        while (iter.hasNext) {
          val next = iter.next()
          sb.append(s)
          sb.append(next.getStringValue)
          s = " "
        }
      } else {
        sb.append(part)
      }
      template = !template
    }

    sb.toString
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val expr = new XProcVtExpression(exprContext, text)
    val evaluator = config.expressionEvaluator.newInstance()
    val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      next match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(next.getStringValue)
      }
    }
  }

  protected def xpathValue(expr: XProcExpression): XdmValue = {
    val eval = config.expressionEvaluator.newInstance()
    val msg = eval.singletonValue(expr, contextItem.toList, msgBindings, None)
    msg.item
  }
}
