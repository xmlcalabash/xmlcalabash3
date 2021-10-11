package com.xmlcalabash.runtime

import com.jafpl.graph.BindingParams
import com.jafpl.messages.{ExceptionMessage, Message, PipelineMessage}
import com.jafpl.runtime.ExpressionEvaluator
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.util.MediaType
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{CollectionFinder, Resource, ResourceCollection}
import net.sf.saxon.ma.arrays.ArrayItem
import net.sf.saxon.om.{Item, SpaceStrippingRule}
import net.sf.saxon.s9api.{ItemTypeFactory, QName, SaxonApiException, SaxonApiUncheckedException, SequenceType, XPathExecutable, XdmArray, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.trans.XPathException
import net.sf.saxon.tree.tiny.TinyAttributeImpl
import net.sf.saxon.value.StringValue
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsScala, IteratorHasAsJava}
import scala.util.DynamicVariable

object SaxonExpressionEvaluator {
  val DEFAULT_COLLECTION = "https://xmlcalabash.com/default-collection"
  protected val _dynContext = new DynamicVariable[DynamicContext](null)
}

class SaxonExpressionEvaluator(xmlCalabash: XMLCalabashConfig) extends ExpressionEvaluator {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def withContext[T](context: DynamicContext)(thunk: => T): T = SaxonExpressionEvaluator._dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(SaxonExpressionEvaluator._dynContext.value)

  override def newInstance(): SaxonExpressionEvaluator = {
    new SaxonExpressionEvaluator(xmlCalabash)
  }

  override def singletonValue(xpath: Any, context: List[Message], bindings: Map[String, Message], params: Option[BindingParams]): XdmValueItemMessage = {
    val xdmval = value(xpath, context, bindings, params).item.asInstanceOf[XdmValue]

    if (xdmval.size() == 1) {
      new XdmValueItemMessage(xdmval, XProcMetadata.XML, xpath.asInstanceOf[XProcExpression].context)
    } else {
      xpath match {
        case xpath: XProcXPathExpression =>
          throw XProcException.xiSeqNotSupported(xpath.context.location, xpath)
        case xpath: XProcVtExpression =>
          val viter = xdmval.iterator()
          var s = ""
          var pos = 0
          while (viter.hasNext) {
            if (pos > 0) {
              s += " "
            }
            s += viter.next().getStringValue
          }
          new XdmValueItemMessage(xdmval, XProcMetadata.XML, xpath.context)
        // We should never fall off the end of this match because we got past the value() call above
      }
    }
  }

  override def value(xpath: Any, context: List[Message], bindings: Map[String, Message], params: Option[BindingParams]): XdmValueItemMessage = {
    val proxies = mutable.HashMap.empty[Any, XdmItem]

    // FIXME: this is ugly
    val exprContext = xpath match {
      case expr: XProcXPathExpression => Some(expr.context)
      case expr: XProcVtExpression => Some(expr.context)
      case _ => None
    }

    val newContext = if (exprContext.isDefined) {
      new DynamicContext(exprContext.get.artifact)
    } else {
      new DynamicContext()
    }

    for (message <- context) {
      message match {
        case msg: XdmValueItemMessage =>
          msg.item match {
            case item: XdmItem =>
              val node = proxy(msg)
              proxies.put(item, node)
              checkDocument(newContext, node, msg)
            case _ =>
              () // Whatever it is, it isn't a document
          }
        case msg: AnyItemMessage =>
          proxies.put(msg.shadow, msg.item)
          checkDocument(newContext, msg.item, msg)
        case _ => ()
      }
    }

    for (message <- bindings.values) {
      message match {
        case msg: XdmValueItemMessage =>
          msg.item match {
            case item: XdmItem =>
              val node = proxy(msg)
              proxies.put(item, node)
              checkDocument(newContext, node, msg)
            case _ =>
              () // Whatever it is, it isn't a document
          }
        case msg: AnyItemMessage =>
          proxies.put(msg.shadow, msg.item)
          checkDocument(newContext, msg.item, msg)
        case _ => ()
      }
    }

    val options = if (params.isDefined && params.get.isInstanceOf[XPathBindingParams]) {
      params.get.asInstanceOf[XPathBindingParams]
    } else {
      XPathBindingParams.EMPTY
    }

    /*
    val options = xpath match {
      case xp: XProcXPathExpression => xp.context.options
      case xp: XProcVtExpression => xp.context.options
      case _ => None
    }

    if (options.isDefined) {
      options.get match {
        case seo: SaxonExpressionOptions =>
          if (seo.inj_elapsed.isDefined) {
            newContext.injElapsed = seo.inj_elapsed.get
          }
          if (seo.inj_id.isDefined) {
            newContext.injId = seo.inj_id.get
          }
          if (seo.inj_name.isDefined) {
            newContext.injName = seo.inj_name.get
          }
          if (seo.inj_type.isDefined) {
            newContext.injType = seo.inj_type.get
          }
      }
    }
    */

    var xdmvalue = Option.empty[XdmValue]

    xpath match {
      case xpath: XProcXPathExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case xpath: XProcVtExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case xpath: XProcXPathValue =>
        xdmvalue = Some(xpath.value.value)
      case _ =>
        throw XProcException.xiNotAnXPathExpression(xpath, None)
    }

    if (xdmvalue.isEmpty) {
      for ((str, value) <- bindings) {
        value match {
          case msg: XdmValueItemMessage =>
            msg.item match {
              case item: XdmItem =>
                checkDocument(newContext, item, value)
              case _ =>
                () // Whatever this is, it isn't a document
            }
          case _ =>
            throw XProcException.xiInvalidMessage(newContext.location, value)
        }
      }

      xdmvalue = Some(withContext(newContext) { compute(xpath.asInstanceOf[XProcExpression], context, bindings, proxies.toMap, options) })
    }

    val metadata = xdmvalue.get match {
      case node: XdmNode =>
        val baseURI = new XdmAtomicValue(node.getBaseURI)
        val pmap = mutable.Map.empty[QName,XdmItem]
        pmap.put(XProcConstants._base_uri, baseURI)
        new XProcMetadata(MediaType.XML, pmap.toMap)
      case _ => ()
        // I'm suspicious that this isn't right in the general case
        XProcMetadata.XML
    }

    new XdmValueItemMessage(xdmvalue.get, metadata, xpath.asInstanceOf[XProcExpression].context)
  }

  override def booleanValue(xpath: Any, context: List[Message], bindings: Map[String, Message], params: Option[BindingParams]): Boolean = {
    val xdmval = value(xpath, context, bindings, params).item

    if (xdmval.size() == 0) {
      //System.err.println(s"$xpath = false")
      return false
    }

    if (xdmval.size() == 1) {
      xdmval.itemAt(0) match {
        case atomic: XdmAtomicValue =>
          //System.err.println(s"$xpath = ${atomic.getBooleanValue}")
          atomic.getUnderlyingValue.effectiveBooleanValue()
        case _ =>
          //System.err.println(s"$xpath = true")
          true
      }
    } else {
      //System.err.println(s"$xpath = true")
      true
    }
  }

  def compute(xpath: XProcExpression, context: List[Message], bindings: Map[String, Message],
              proxies: Map[Any,XdmItem], options: XPathBindingParams): XdmValue = {
    val patchBindings = mutable.HashMap.empty[QName, XdmValue]

    for ((str, value) <- xpath.context.statics) {
      value match {
        case msg: XdmValueItemMessage =>
          patchBindings.put(ValueParser.parseClarkName(str), msg.item)
        case _ =>
          throw XProcException.xiInvalidMessage(None, value)
      }
    }

    for ((str, value) <- bindings) {
      value match {
        case msg: XdmValueItemMessage =>
          patchBindings.put(ValueParser.parseClarkName(str), msg.item)
        case _ =>
          throw XProcException.xiInvalidMessage(None, value)
      }
    }

    xpath match {
      case avtexpr: XProcVtExpression =>
        val xdmresults = ListBuffer.empty[XdmValue]
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            xdmresults += computeValue(part, None, context, avtexpr.context, patchBindings.toMap, proxies, avtexpr.extensionFunctionsAllowed, options, false)
          } else {
            if (part != "") {
              xdmresults += new XdmAtomicValue(part)
            }
          }
          evalAvt = !evalAvt
        }

        // Special case for AVT=""
        if (xdmresults.isEmpty) {
          xdmresults += new XdmAtomicValue("")
        }

        var xdmval: XdmValue = null;
        if (avtexpr.stringResult) {
          // Make sure we get spaces in the right places
          val sbuf = new StringBuffer()
          for (part <- xdmresults) {
            val viter = part.iterator()
            var first = true
            while (viter.hasNext) {
              val v = viter.next()
              if (!first) {
                // Needed to pass ab-avt-009 but I'm not sure what the rule is...
                sbuf.append(" ");
              }
              first = false
              sbuf.append(avtStringOf(v.getUnderlyingValue))
            }
          }

          val typeFactory = new ItemTypeFactory(xmlCalabash.processor)
          val untypedAtomicType = typeFactory.getAtomicType(XProcConstants.xs_untypedAtomic)
          xdmval = new XdmAtomicValue(sbuf.toString, untypedAtomicType)
        } else {
          for (part <- xdmresults) {
            if (xdmval == null) {
              xdmval = part;
            } else {
              xdmval = xdmval.append(part);
            }
          }
        }

        xdmval
      case xpathexpr: XProcXPathExpression =>
        val collection = if (xpathexpr.params.isDefined) {
          xpathexpr.params.get.collection
        } else {
          false
        }

        val result = computeValue(xpathexpr.expr, xpathexpr.as, context, xpathexpr.context, patchBindings.toMap, proxies, xpathexpr.extensionFunctionsAllowed, options, collection)
        result
      case _ =>
        throw XProcException.xiUnexpectedExprType(xpath.context.location, xpath)
    }
  }

  private def avtStringOf(item: Item): String = {
    item match {
      case arr: ArrayItem =>
        val buf = new StringBuffer()
        for (pos <- 0 until arr.arrayLength()) {
          if (pos > 0) {
            buf.append(" ")
          }
          for (v <- arr.get(pos).asIterable().asScala) {
            buf.append(avtStringOf(v))
          }
        }
        buf.toString
      case _ =>
        try {
          item.getStringValue
        } catch {
          case ex: Throwable =>
            throw XProcException.xdGeneralError(ex.getMessage, None)
        }
    }
  }

  private def computeValue(xpath: String,
                           as: Option[SequenceType],
                           contextItem: List[Message],
                           exprContext: StaticContext,
                           bindings: Map[QName,XdmValue],
                           proxies: Map[Any, XdmItem],
                           extensionsOk: Boolean,
                           params: XPathBindingParams,
                           useCollection: Boolean): XdmValue = {
    val config = xmlCalabash.processor.getUnderlyingConfiguration

    val sconfig = xmlCalabash.processor.getUnderlyingConfiguration
    val curfinder = sconfig.getCollectionFinder

    if (useCollection) {
      sconfig.setCollectionFinder(new ExprCollectionFinder(curfinder, contextItem))
      sconfig.setDefaultCollection(SaxonExpressionEvaluator.DEFAULT_COLLECTION)
    }

    try {
      val xcomp = xmlCalabash.processor.newXPathCompiler()
      val baseURI = if (exprContext.baseURI.isDefined) {
        exprContext.baseURI.get
      } else {
        new URI("")
      }
      if (baseURI.toASCIIString != "") {
        xcomp.setBaseURI(baseURI)
      }

      for (varname <- bindings.keySet) {
        xcomp.declareVariable(varname)
      }
      for (varname <- params.statics.keySet) {
        if (!bindings.contains(varname)) {
          xcomp.declareVariable(varname)
        }
      }

      for ((prefix, uri) <- exprContext.nsBindings) {
        xcomp.declareNamespace(prefix, uri)
      }

      var xexec: XPathExecutable = null // Yes, I know.
      try {
        xexec = xcomp.compile(xpath)
      } catch {
        case sae: SaxonApiException =>
          sae.getCause match {
            case _: XPathException =>
              throw XProcException.xsStaticErrorInExpression(xpath, sae.getMessage, exprContext.location)
            case _ => throw sae
          }
        case other: Throwable =>
          throw other
      }

      val selector = xexec.load()

      for ((varname, varvalue) <- bindings) {
        selector.setVariable(varname, varvalue)
      }
      for ((varname, varvalue) <- params.statics) {
        if (!bindings.contains(varname)) {
          selector.setVariable(varname, varvalue)
        }
      }

      if (contextItem.size == 1) {
        contextItem.head match {
          case msg: XdmValueItemMessage =>
            selector.setContextItem(proxies(msg.item))
          case msg: PipelineMessage =>
            msg.item match {
              case item: XdmItem =>
                if (proxies.contains(item)) {
                  selector.setContextItem(proxies(item))
                } else {
                  selector.setContextItem(item)
                }
              case _ =>
                throw new RuntimeException(s"Impossible to set context item to ${msg.item}")
            }
          case msg: AnyItemMessage =>
            selector.setContextItem(proxies(msg.shadow))
          case msg: ExceptionMessage =>
            msg.item match {
              case ex: XProcException =>
                if (ex.errors.isDefined) {
                  selector.setContextItem(ex.errors.get)
                }
              case _ =>
                throw new RuntimeException(s"Impossible to set context item to ${msg.item}")
            }
          case _ =>
            throw new RuntimeException(s"Impossible to set context item to ${contextItem.head}")
        }
      }

      var value: XdmValue = null;
      try {
        value = selector.evaluate()
      } catch {
        case sae: SaxonApiException =>
          if (sae.getMessage.contains("context item") && sae.getMessage.contains("absent")) {
            if (contextItem.size > 1) {
              throw XProcException.xdContextItemSequence(xpath, sae.getMessage, exprContext.location)
            } else {
              throw XProcException.xdContextItemAbsent(xpath, sae.getMessage, exprContext.location)
            }
          } else {
            val msg = sae.getMessage
            if (msg.contains("Invalid JSON")) {
              throw XProcException.xdInvalidJson(sae.getMessage, exprContext.location)
            } else if (msg.contains("Namespace prefix") && msg.contains("has not been declared")) {
              throw XProcException.xdMissingNamespaceBinding(msg, exprContext.location)
            } else {
              throw XProcException.xdGeneralError(sae.getMessage, exprContext.location)
            }
          }
        case ex: Exception =>
          throw ex
      }

      // Special case?
      var uvalue = value.getUnderlyingValue
      uvalue match {
        case _: TinyAttributeImpl =>
          uvalue = new XdmAtomicValue(uvalue.getStringValue).getUnderlyingValue
        case _ => ()
      }

      if (as.isDefined) {
        val matches = as.get.getUnderlyingSequenceType.matches(uvalue, config.getTypeHierarchy)
        if (!matches) {
          throw XProcException.xdBadType(uvalue.toString, as.get.toString, exprContext.location)
        }
      }

      if (useCollection) {
        sconfig.setCollectionFinder(curfinder)
      }

      value
    } catch {
      case saue: SaxonApiUncheckedException =>
        saue.getCause match {
          case xpe: XPathException =>
            val code = new QName(xpe.getErrorCodeNamespace, xpe.getErrorCodeLocalPart)
            throw XProcException.xcGeneralException(code, xpe, None, exprContext.location)
          case _ => throw saue
        }
      case ex: Exception =>
        throw ex
    }
  }

  // =========================================================================================================

  def checkDocument(dynContext: DynamicContext, item: XdmItem, msg: Message): Unit = {
    item match {
      case node: XdmNode =>
        var p: XdmNode = node
        while (Option(p.getParent).isDefined) {
          p = p.getParent
        }

        if (p.getNodeKind == XdmNodeKind.DOCUMENT) {
          dynContext.addDocument(p, msg)
        }
      case item: XdmItem =>
        dynContext.addItem(item.getUnderlyingValue, msg)
      case _ => ()
    }
  }

  private def proxy(message: Message): XdmItem = {
    message match {
      case msg: XdmValueItemMessage =>
        msg.item match {
          case item: XdmItem => return item
          case _ => ()
        }

        msg.metadata match {
          case xproc: XProcMetadata =>
            val props = xproc.properties
            val builder = new SaxonTreeBuilder(xmlCalabash)
            builder.startDocument(None)
            builder.addStartElement(XProcConstants.c_document_properties)
            for ((key,value) <- props) {
              builder.addStartElement(key)
              value match {
                case atom: XdmAtomicValue => builder.addValues(value)
                case node: XdmNode => builder.addSubtree(node)
                case _ => throw XProcException.xiInvalidPropertyValue(value, None)
              }
              builder.addEndElement()
            }
            builder.addEndElement()
            builder.endDocument()
            builder.result
          case _ => emptyProxy()
        }
      case _ => emptyProxy()
    }
  }

  private def emptyProxy(): XdmNode = {
    val builder = new SaxonTreeBuilder(xmlCalabash)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_document_properties)
    builder.addEndElement()
    builder.endDocument()
    builder.result
  }

  class ExprCollectionFinder(finder: CollectionFinder, messages: List[Message]) extends CollectionFinder {
    private val items = ListBuffer.empty[ExprNodeResource]

    for (msg <- messages) {
      val node = dynContext.get.document(msg)
      if (node.isDefined) {
        items += new ExprNodeResource(node.get)
      } else {
        msg match {
          case node: XdmNodeItemMessage =>
            items += new ExprNodeResource(node.item)
          case item: XdmValueItemMessage =>
            () // ???
          case _ =>
            throw XProcException.xiBadMessage(msg, None)
        }
      }
    }
    private val rsrcColl = new ExprResourceCollection(items.toList)

    override def findCollection(context: XPathContext, collectionURI: String): ResourceCollection = {
      if (collectionURI == SaxonExpressionEvaluator.DEFAULT_COLLECTION) {
        rsrcColl
      } else {
        finder.findCollection(context, collectionURI)
      }
    }
  }

  class ExprResourceCollection(items: List[ExprNodeResource]) extends ResourceCollection {
    override def getCollectionURI: String = ""

    override def getResourceURIs(context: XPathContext): util.Iterator[String] = {
      val uris = ListBuffer.empty[String]
      for (rsrc <- items) {
        uris += rsrc.getResourceURI
      }
      uris.iterator.asJava
    }

    override def getResources(context: XPathContext): util.Iterator[_ <: Resource] = {
      items.iterator.asJava
    }

    override def stripWhitespace(rules: SpaceStrippingRule): Boolean = {
      false
    }

    override def isStable(context: XPathContext): Boolean = {
      true
    }
  }

  class ExprNodeResource(uri: String, item: Item) extends Resource {
    def this(node: XdmNode) = {
      this(node.getBaseURI.toASCIIString, node.getUnderlyingNode)
    }
    def this(value: XdmValue) = {
      this(null, value.getUnderlyingValue.head)
    }

    override def getResourceURI: String = uri
    override def getItem(context: XPathContext): Item = item
    override def getContentType: String = null
  }
}
