package com.xmlcalabash.model.xxml

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants.{ValueTemplate, _content_type}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants, XValueParser}
import com.xmlcalabash.runtime.params.DocumentLoaderParams
import com.xmlcalabash.runtime.{StaticContext, XProcVtExpression}
import com.xmlcalabash.util.{MediaType, Urify}
import net.sf.saxon.s9api.QName
import net.sf.saxon.sapling.Saplings.doc

import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XDocument(config: XMLCalabash) extends XDataSource(config) {
  private var _href: String = _
  private var _hrefAvt: ValueTemplate = _
  private var _contentType = Option.empty[MediaType]
  private var _documentProperties = Option.empty[String]
  private var _parameters = Option.empty[String]
  private var _contextDependent = false

  def contextDependent: Boolean = _contextDependent

  protected[xxml] def this(parent: XArtifact, href: String) = {
    this(parent.config)
    this.parent = parent
    this.staticContext = parent.staticContext
    _synthetic = true
    _href = href
    _hrefAvt = XValueParser.parseAvt(_href)
  }

  protected[xxml] def this(parent: XArtifact, copy: XDocument) = {
    this(copy.config)
    _href = copy._href
    _hrefAvt = copy._hrefAvt
    _contentType = copy._contentType
    _documentProperties = copy._documentProperties
    _parameters = copy._parameters
    _contextDependent = copy._contextDependent
    _drp = copy._drp
    staticContext = copy.staticContext
    this.parent = parent
  }

  def loaderParams: DocumentLoaderParams = {
    // FIXME: context_provided is always false
    new DocumentLoaderParams(_hrefAvt, _contentType, _parameters, _documentProperties, _contextDependent, staticContext)
  }

  def loadDocument(): DocumentRequest = {
    val expr = new XProcVtExpression(staticContext, _hrefAvt, true)
    val msg = config.expressionEvaluator.value(expr, List(), staticContext.inscopeConstantBindings, None)
    val href = if (staticContext.baseURI.isDefined) {
      staticContext.baseURI.get.resolve(msg.item.toString)
    } else {
      new URI(msg.item.toString)
    }
    new DocumentRequest(href, _contentType, location)
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    if (attributes.contains(XProcConstants._href)) {
      _href = attr(XProcConstants._href).get
    } else {
      error(XProcException.xsMissingRequiredAttribute(XProcConstants._href, None))
    }
    _contentType = parseContentType()
    _documentProperties = attr(XProcConstants._document_properties)
    _parameters = attr(XProcConstants._parameters)
    _hrefAvt = XValueParser.parseAvt(_href)
  }

  override protected[xxml] def validate(): Unit = {
    if (!synthetic) {
      checkAttributes()
      checkEmptyAttributes()
    }

    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }
    allChildren = List()
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    super.elaborateDefaultReadablePort(initial)
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    try {
      val refs = mutable.HashSet.empty[QName]

      val parser = new XValueParser(config, staticContext, _hrefAvt)
      refs ++= parser.variables
      _contextDependent = _contextDependent || parser.contextDependent

      if (_documentProperties.isDefined) {
        val parser = new XValueParser(config, staticContext, _documentProperties.get)
        refs ++= parser.variables
        _contextDependent = _contextDependent || parser.contextDependent
      }

      if (_parameters.isDefined) {
        val parser = new XValueParser(config, staticContext, _parameters.get)
        refs ++= parser.variables
        _contextDependent = _contextDependent || parser.contextDependent
      }

      resolveBindings(_contextDependent, refs.toSet, initial)
    } catch {
      case ex: Exception =>
        error(ex)
    }

    staticContext = staticContext.withConstants(initial)

    initial
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("href", Option(_href))

    if (drp.isDefined) {
      attr.put("drp", Some(drp.get.tumble_id))
    } else {
      attr.put("drp", Some("!undefined"))
    }

    dumpTree(sb, "p:document", attr.toMap)
  }
}
