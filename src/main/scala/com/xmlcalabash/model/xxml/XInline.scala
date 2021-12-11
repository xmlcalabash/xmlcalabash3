package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants, XValueParser}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import java.io.UnsupportedEncodingException
import java.util.Base64
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala

class XInline(config: XMLCalabash, val content: XdmNode, implied: Boolean) extends XDataSource(config) {
  private var _contentType = Option.empty[MediaType]
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  private var _exclude_inline_prefixes = Option.empty[String]
  private var _expandText = false
  private val _excludeURIs = mutable.HashSet.empty[String]
  private val _valueTemplates = ListBuffer.empty[String]
  private var _contextDependent = false
  _synthetic = implied

  if (Option(content).isEmpty || content.getNodeKind != XdmNodeKind.DOCUMENT) {
    throw XProcException.xiThisCantHappen(s"Attempt to create p:inline from something that isn't a document node: ${content}")
  }

  def this(config: XMLCalabash, parent: XArtifact, srcNode: XdmNode) = {
    this(config, srcNode, true)
    this.parent = parent
    staticContext = parent.staticContext
  }

  protected[xxml] def this(parent: XArtifact, copy: XInline) = {
    this(copy.config, copy.content, copy.synthetic)
    _contentType = copy._contentType
    _documentProperties = copy._documentProperties
    _encoding = copy._encoding
    _exclude_inline_prefixes = copy._exclude_inline_prefixes
    _expandText = copy._expandText
    _excludeURIs ++= copy._excludeURIs
    _valueTemplates ++= copy._valueTemplates
    _contextDependent = copy._contextDependent
    _drp = copy._drp
    staticContext = copy.staticContext
    this.parent = parent
  }

  def contentType: Option[MediaType] = _contentType

  def encoding: Option[String] = _encoding

  def documentProperties: Option[String] = _documentProperties

  def expandText: Boolean = _expandText

  def excludeURIs: Set[String] = _excludeURIs.toSet

  def contextDependent: Boolean = _contextDependent

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    val saveContentType = attributes.get(XProcConstants._content_type)

    _contentType = parseContentType()
    _documentProperties = attr(XProcConstants._document_properties)
    _encoding = attr(XProcConstants._encoding)
    _exclude_inline_prefixes = attr(XProcConstants._exclude_inline_prefixes)

    if (_contentType.isDefined && _encoding.isEmpty) {
      if (_contentType.get.charset.isDefined) {
        error(XProcException.xdCharsetWithoutEncoding(saveContentType.get, None))
      }
    }

    if (_encoding.isDefined) {
      if (_encoding.get == "base64") {
        if (_contentType.isDefined && _contentType.get.markupContentType) {
          error(XProcException.xdCannotEncodeMarkup(_encoding.get, _contentType.get, None))
        }

        val charset = if (_contentType.isDefined) {
          _contentType.get.charset.getOrElse("UTF-8")
        } else {
          "UTF-8"
        }

        // Can I trust you?
        try {
          // Apparently the decoder won't acept newlines in the data...
          val str = content.getStringValue.trim.replace("\n", "")
          val bytes = Base64.getDecoder.decode(str)
          if (contentType.isDefined && contentType.get.textContentType) {
            new String(bytes, charset)
          }
        } catch {
          case _: IllegalArgumentException =>
            error(XProcException.xdIncorrectEncoding(_encoding.get, None))
          case _: UnsupportedEncodingException =>
            error(XProcException.xdUnsupportedCharset(charset, None))
          case ex: Exception =>
            error(ex)
        }
      } else {
        error(XProcException.xsUnsupportedEncoding(_encoding.get, None))
      }
    }

    if (_contentType.isEmpty) {
      _contentType = Some(MediaType.XML)
    }
  }

  protected[xxml] def parseExpandText(node: XdmNode): Unit = {
    var elem: Option[XdmNode] = Some(node)
    var ename = Option.empty[QName]
    var eattr = Option.empty[String]
    while (elem.isDefined && eattr.isEmpty) {
      elem.get.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (elem.get.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
            ename = Some(XProcConstants._expand_text)
            eattr = Option(elem.get.getAttributeValue(ename.get))
          } else {
            ename = Some(XProcConstants.p_expand_text)
            eattr = Option(elem.get.getAttributeValue(ename.get))
          }
        case _ => ()
      }
      elem = Option(elem.get.getParent)
    }

    val value = eattr.getOrElse("true")
    if (value == "true" || value == "false") {
      _expandText = (value == "true")
    } else {
      error(XProcException.xsInvalidExpandText(ename.get, value, location))
    }
  }

  protected[xxml] def parseExcludedUris(node: XdmNode): Unit = {
    _excludeURIs += XProcConstants.ns_p

    var elem: Option[XArtifact] = Some(this)
    var eattr = Option.empty[String]

    elem = Some(this)
    while (elem.isDefined) {
      if (elem.get.nodeName.getNamespaceURI == XProcConstants.ns_p) {
        eattr = elem.get.attributes.get(XProcConstants._exclude_inline_prefixes)
      } else {
        eattr = elem.get.attributes.get(XProcConstants.p_exclude_inline_prefixes)
      }
      if (eattr.isDefined && elem.get.exceptions.isEmpty) {
        val prefixes = mutable.HashSet.empty[String]
        for (token <- eattr.get.trim.split("\\s+")) {
          prefixes += token
        }

        try {
          _excludeURIs ++= S9Api.urisForPrefixes(node, prefixes.toSet)
        } catch {
          case ex: Exception =>
            elem.get.error(ex)
        }
      }
      elem = elem.get.parent
    }
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    val refs = mutable.HashSet.empty[QName]

    if (expandText) {
      findValueTemplates()

      try {
        for (avt <- _valueTemplates) {
          val parser = new XValueParser(config, staticContext, XValueParser.parseAvt(avt))
          refs ++= parser.variables
          _contextDependent = _contextDependent || parser.contextDependent
        }
      } catch {
        case ex: Exception =>
          error(ex)
      }

      if (exceptions.nonEmpty) {
        return initial
      }

      if (documentProperties.isDefined) {
        try {
          val parser = new XValueParser(config, staticContext, documentProperties.get)
          refs ++= parser.variables
          _contextDependent = _contextDependent || parser.contextDependent
        } catch {
          case ex: Exception =>
            error(ex)
        }
      }

      resolveBindings(_contextDependent, refs.toSet, initial)

      staticContext = staticContext.withConstants(initial)
    }




    initial
  }

  private def findValueTemplates(): Unit = {
    findValueTemplates(content, expandText)
  }

  private def findValueTemplates(node: XdmNode, expand: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- node.axisIterator(Axis.CHILD).asScala) {
          findValueTemplates(child, expand)
        }
      case XdmNodeKind.ELEMENT =>
        val inlineExpand = if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
          Option(node.getAttributeValue(XProcConstants._inline_expand_text))
        } else {
          Option(node.getAttributeValue(XProcConstants.p_inline_expand_text))
        }
        val expandNode = if (inlineExpand.isDefined) {
          inlineExpand.get == "true"
        } else {
          expand
        }
        if (expandNode) {
          for (attr <- node.axisIterator(Axis.ATTRIBUTE).asScala) {
            if (attr.getStringValue.contains("{")) {
              _valueTemplates += attr.getStringValue
            }
          }
        }
        for (child <- node.axisIterator(Axis.CHILD).asScala) {
          findValueTemplates(child, expandNode)
        }
      case XdmNodeKind.TEXT =>
        if (expand) {
          if (node.getStringValue.contains("{")) {
            _valueTemplates += node.getStringValue
          }
        }
      case _ => ()
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("content-type", _contentType)
    attr.put("document-properties", _documentProperties)
    attr.put("encoding", _encoding)
    attr.put("exclude-inline-prefixes", _exclude_inline_prefixes)
    attr.put("expand-text", Some(_expandText))
    dumpTree(sb, "p:inline", attr.toMap)
  }
}
