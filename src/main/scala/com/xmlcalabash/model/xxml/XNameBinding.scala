package com.xmlcalabash.model.xxml

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{XProcConstants, XValueParser}
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.steps.internal.ValueComputation
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, S9Api, TypeUtils}
import net.sf.saxon.ma.map.{MapItem, MapType}
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, SequenceType, XdmAtomicValue, XdmMap}

import java.net.URISyntaxException
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XNameBinding {
  def checkValueTokens(config: XMLCalabash,
                       context: MinimalStaticContext,
                       values: Option[String]): Option[List[XdmAtomicValue]] = {
    if (values.isDefined) {
      val exprEval = config.expressionEvaluator.newInstance()

      val expr = new XProcXPathExpression(context, values.get, None, None, None)
      val value = exprEval.value(expr, List(), Map(), None)
      val iter = value.item.iterator()
      val allowed = ListBuffer.empty[XdmAtomicValue]
      while (iter.hasNext) {
        val token = iter.next()
        token match {
          case atom: XdmAtomicValue =>
            allowed += atom
          case _ =>
            throw XProcException.xsInvalidValues(values.get, None)
        }
      }

      return Some(allowed.toList)
    }

    None
  }

  def promotedValue(config: XMLCalabash,
                    name: QName,
                    declaredType: Option[SequenceType],
                    tokenList: Option[List[XdmAtomicValue]],
                    staticValueMsg: XdmValueItemMessage): XdmValueItemMessage = {
    val typeUtils = new TypeUtils(config, staticValueMsg.context)
    try {
      typeUtils.convertType(name, staticValueMsg, declaredType, tokenList)
    } catch {
      case ex: XProcException =>
        if (ex.code == XProcException.errxd(19)) {
          throw ex
        } else {
          // FIXME: if this was a namespace error, we lose critical information about the error
          throw XProcException.xdBadType(name, staticValueMsg.item.toString, declaredType.get.toString, None)
        }
      case ex: URISyntaxException =>
        val dtype = declaredType.get.getItemType.getTypeName
        if (dtype == XProcConstants.xs_anyURI) {
          throw XProcException.xdInvalidURI(staticValueMsg.item.toString, None)
        } else {
          throw ex
        }
      case ex: Exception =>
        throw ex
    }
  }
}

abstract class XNameBinding(config: XMLCalabash) extends XArtifact(config) {
  private val structured_xs_QName = new StructuredQName("xs", XProcConstants.ns_xs, "QName")

  protected var _qnameKeys: Boolean = false
  protected var _name: QName = _
  protected var _as = Option.empty[String]
  protected var _declaredType = Option.empty[SequenceType]
  protected var _values = Option.empty[String]
  protected var _static = Option.empty[Boolean]
  protected var _constant = false
  protected var _required = Option.empty[Boolean]
  protected var _select = Option.empty[String]
  protected var _avt = Option.empty[String]
  protected var _visibility = Option.empty[String]
  protected var _variableReferences: Set[QName] = Set()

  protected var _constantValue = Option.empty[XdmValueItemMessage]
  protected var _computeValue = Option.empty[ValueComputation]
  protected var collection = false

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  private var _drp: Option[XPort] = None

  def name: QName = _name

  protected[xxml] def drp: Option[XPort] = _drp

  protected[xxml] def drp_=(port: Option[XPort]): Unit = {
    _drp = port
  }

  def static: Boolean = _static.getOrElse(false)
  def constant: Boolean = _constant
  def visibility: String = _visibility.getOrElse("public")

  def constantValue: Option[XdmValueItemMessage] = _constantValue

  def as: Option[String] = _as
  def declaredType: Option[SequenceType] = _declaredType

  def required: Boolean = _required.getOrElse(false)

  def select: Option[String] = _select

  def qnameKeys: Boolean = _qnameKeys

  def usedByPipeline: Boolean = {
    children[XWithOutput].head.readBy.nonEmpty
  }

  override protected[xxml] def checkAttributes(): Unit = {
    if (synthetic) {
      return
    }

    super.checkAttributes()

    try {
      if (attributes.contains(XProcConstants._name)) {
        val name = attr(XProcConstants._name).get
        try {
          _name = staticContext.parseQName(name)
        } catch {
          case ex: XProcException =>
            if (ex.code == XProcException.err_xd0015) {
              error(XProcException.xsOptionUndeclaredNamespace(name, ex.location))
            } else {
              error(ex)
            }
        }

        this match {
          case _: XWithOption =>
            () // This would be ok if the step has an option declared in the p: namespace
          case _ =>
            if (_name.getNamespaceURI == XProcConstants.ns_p) {
              error(XProcException.xsOptionInXProcNamespace(_name, None))
            }
        }
      } else {
        error(XProcException.xsMissingRequiredAttribute(XProcConstants._name, None))
      }

      _as = attr(XProcConstants._as)
      _declaredType = staticContext.parseSequenceType(_as, config.itemTypeFactory)

      if (declaredType.isDefined)
        declaredType.get.getUnderlyingSequenceType.getPrimaryType match {
          case map: MapType =>
            if (map.getKeyType.getPrimitiveItemType.getTypeName == structured_xs_QName) {
              // We have to lie about the type of maps with QName keys because we're
              // going to allow users to put strings in there.
              _qnameKeys = true
              _declaredType = Some(staticContext.parseFakeMapSequenceType(as.get, config.itemTypeFactory))
            }
          case _ => ()
        }

      _static = staticContext.parseBoolean(attr(XProcConstants._static))
      _required = staticContext.parseBoolean(attr(XProcConstants._required))
      _select = attr(XProcConstants._select)
      _visibility = attr(XProcConstants._visibility)

      if (visibility != "public" && visibility != "private") {
        error(XProcException.xsBadVisibility(visibility, location))
      }

      if (_required.isDefined && _required.get && _select.isDefined) {
        error(XProcException.xsRequiredAndDefaulted(_name, location))
      }

      if (_required.isDefined && _required.get && static) {
        error(XProcException.xsRequiredAndStatic(_name, location))
      }

      val _collection = attr(XProcConstants._collection)
      if (_collection.isDefined) {
        val coll = _collection.get
        if (coll == "true" || coll == "false") {
          collection = coll == "true"
        } else {
          error(XProcException.xsBadTypeValue(coll, "xs:boolean", None))
        }
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  protected def checkValueTokens: Option[List[XdmAtomicValue]] = {
    _values = attr(XProcConstants._values)
    if (_values.isDefined) {
      val exprEval = config.expressionEvaluator.newInstance()

      val expr = new XProcXPathExpression(staticContext, _values.get, None, None, None)
      val value = exprEval.value(expr, List(), Map(), None)
      val iter = value.item.iterator()
      val allowed = ListBuffer.empty[XdmAtomicValue]
      while (iter.hasNext) {
        val token = iter.next()
        token match {
          case atom: XdmAtomicValue =>
            allowed += atom
          case _ =>
            error(XProcException.xsInvalidValues(_values.get, None))
        }
      }

      return Some(allowed.toList)
    }

    None
  }

  protected def valueParser(): XValueParser = {
    if (_avt.isDefined) {
      new XValueParser(config, staticContext, XValueParser.parseAvt(_avt.get))
    } else if (_select.isDefined) {
      new XValueParser(config, staticContext, _select.get)
    } else {
      throw XProcException.xsInvalidPipeline(s"No value provided for ${name}", location)
    }
  }

  protected[xxml] def graphEdges(runtime: XMLCalabashRuntime): Unit = {
    if (constantValue.isDefined) {
      return
    }

    for (input <- children[XWithInput]) {
      for (child <- input.allChildren) {
        child match {
          case pipe: XPipe =>
            pipe.graphEdges(runtime)
          case _: XWithOutput =>
            ()
          case _ =>
            throw XProcException.xiThisCantHappen(s"Name binding has unexpected child: ${child}")
        }
      }
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    super.elaborateDefaultReadablePort(initial)
    _drp
  }

  override protected[xxml] def elaboratePortConnections(): Unit = {
    // This smells bad. Clearly it's defending against the case where there are no XDataSources
    // in the children but there's a DRP. Except in the case of variable, if there is an XDataSource,
    // it'll be in a XWithInput. So why are there ever "naked" XDataSource elements in the
    // children?
    if (drp.isDefined && children[XDataSource].isEmpty && children[XWithInput].isEmpty) {
      addChild(new XPipe(drp.get))
    }

    super.elaboratePortConnections()

    // Now collect any unwrapped data sources under a p:with-input
    val newChildren = ListBuffer.empty[XArtifact]
    val input = if (children[XWithInput].isEmpty) {
      val xwi = new XWithInput(this, "source")
      xwi.sequence = true
      xwi.primary = false
      xwi.contentTypes = MediaType.MATCH_ANY
      xwi
    } else {
      children[XWithInput].head
    }
    newChildren += input

    for (child <- allChildren) {
      child match {
        case xwi: XWithInput =>
          if (xwi ne input) {
            throw XProcException.xiThisCantHappen("p:with-option has more than one p:with-input child?")
          }
        case ds: XDataSource =>
          input.addChild(ds)
        case _ =>
          newChildren += child
      }
    }

    allChildren = newChildren.toList
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    var bcontext = initial

    if (_avt.isDefined || _select.isDefined) {
      try {
        val parser = valueParser()
        _variableReferences = parser.variables
        val contextDependent = parser.contextDependent

        _constant = !(this.isInstanceOf[XOption])      // p:options can never be constants
        val namepipe = ListBuffer.empty[XNameBinding]
        for (ref <- _variableReferences) {
          val cbind = initial.inScopeConstants.get(ref)
          val dbind = initial.inScopeDynamics.get(ref)
          if (cbind.isDefined) {
            // ok
          } else if (dbind.isDefined) {
            _constant = false
            dbind.get match {
              case v: XVariable =>
                namepipe += v
              case opt: XOption =>
                namepipe += opt
              case _ =>
                error(XProcException.xiThisCantHappen(s"Unexpected name binding: ${dbind.get}"))
            }
          } else {
            error(XProcException.xsNoBindingInExpression(ref, None))
          }
        }

        if (exceptions.nonEmpty) {
          return initial
        }

        if (contextDependent) {
          _constant = false
        } else {
          drp = None // irrelevant
        }

        if (constant) {
          val expr = if (_avt.isDefined) {
            new XProcVtExpression(staticContext, XValueParser.parseAvt(_avt.get), true)
          } else {
            new XProcXPathExpression(staticContext, _select.get)
          }

          val bindings = mutable.HashMap.empty[String,Message]
          for ((name,value) <- initial.inScopeConstants) {
            bindings.put(name.getClarkName, value.constantValue.get)
          }

          var computed = config.expressionEvaluator.value(expr, List(), bindings.toMap, None)
          if (qnameKeys) {
            computed.item match {
              case xmap: XdmMap =>
                val qnameMap = S9Api.forceQNameKeys(xmap.getUnderlyingValue, computed.context)
                computed = new XdmValueItemMessage(qnameMap, computed.metadata, computed.context)
              case xmap: MapItem =>
                val qnameMap = S9Api.forceQNameKeys(xmap, computed.context)
                computed = new XdmValueItemMessage(qnameMap, computed.metadata, computed.context)
              case _ =>
                throw XProcException.xiThisCantHappen(s"Non-map item has qnameKeys: ${computed.item}")
            }
          }

          _constantValue = Some(promotedStaticValue(computed))
          bcontext = bcontext.withBinding(this)
        } else {
          if (namepipe.nonEmpty) {
            val xwi = new XWithInput(this, "#bindings")
            xwi.sequence = true
            xwi.primary = false
            xwi.contentTypes = MediaType.MATCH_ANY
            addChild(xwi)
            for (binding <- namepipe) {
              val xstep = bcontext.inScopeDynamics.get(binding.name)
              val pipe = new XPipe(xwi, xstep.get.tumble_id, "result")
              xwi.addChild(pipe)
            }
          }
        }
      } catch {
        case ex: Exception =>
          error(ex)
      }
    }

    super.elaborateNameBindings(initial)

    bcontext
  }

  protected def promotedStaticValue(staticValueMsg: XdmValueItemMessage): XdmValueItemMessage = {
    val sig = stepDeclaration
    val optdecl = sig.get.option(name).get

    XNameBinding.promotedValue(config, name, optdecl.declaredType, optdecl.tokenList, staticValueMsg)
  }
}