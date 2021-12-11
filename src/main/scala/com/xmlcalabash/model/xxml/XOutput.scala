package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XProcXPathExpression
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmMap}

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class XOutput(config: XMLCalabash) extends XPort(config) {
  private var serializationExpr = Option.empty[String]
  private val _serialization = mutable.Map.empty[QName,String]

  def this(step: XContainer, port: Option[String]) = {
    this(step.config)
    staticContext = step.staticContext
    parent = step
    synthetic = true
    syntheticName = XProcConstants.p_output
    if (port.isDefined) {
      _port = port.get
    }
    _attrChecked = true
  }

  def serialization: Map[QName,String] = {
    _serialization.toMap
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    if (parent.isDefined) {
      parent.get match {
        case _: XDeclareStep =>
          checkDeclareStepAttributes()
        case _ =>
          checkCompoundStepAttributes()
      }
    } else {
      checkCompoundStepAttributes() // This can't happen
    }
  }

  override protected[xxml] def checkEmptyAttributes(): Unit = {
    super.checkEmptyAttributes()
    _attrChecked = true
  }

  private def checkDeclareStepAttributes(): Unit = {
    checkCompoundStepAttributes()
    serializationExpr = attr(XProcConstants._serialization)
  }

  private def checkCompoundStepAttributes(): Unit = {
    try {
      if (attributes.contains(XProcConstants._port)) {
        _port = staticContext.parseNCName(attr(XProcConstants._port)).get
      } else {
        error(XProcException.xsMissingRequiredAttribute(XProcConstants._port, None))
      }

      _sequence = staticContext.parseBoolean(attr(XProcConstants._sequence))
      _primary = staticContext.parseBoolean(attr(XProcConstants._primary))
      _content_types = staticContext.parseContentTypes(attr(XProcConstants._content_types))
      if (_content_types.isEmpty) {
        _content_types = List(MediaType.OCTET_STREAM)
      }

      _href = attr(XProcConstants._href)
      _pipe = attr(XProcConstants._pipe)
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  override protected[xxml] def validate(): Unit = {
    if (!_attrChecked) {
      checkAttributes()
      checkEmptyAttributes()
    }
    super.validate()

    if (parent.isDefined) {
      parent.get match {
        case decl: XDeclareStep =>
          if (decl.atomic && children[XDataSource].nonEmpty) {
            if (decl.stepType.isDefined) {
              error(XProcException.xsAtomicOutputWithBinding(port, decl.stepType.get, location))
            } else {
              error(XProcException.xsAtomicOutputWithBinding(port, location))
            }
          }
        case _ =>
          ()
      }
    }

    if (serializationExpr.isDefined) {
      val exprEval = config.expressionEvaluator.newInstance()
      val expr = new XProcXPathExpression(staticContext, serializationExpr.get)
      val value = try {
        exprEval.value(expr, List(), staticContext.inscopeConstantBindings, None)
      } catch {
        case sae: SaxonApiException =>
          throw XProcException.xsStaticErrorInExpression(serializationExpr.get, sae.getMessage, None)
      }
      value.item match {
        case map: XdmMap =>
          val sermap = S9Api.forceQNameKeys(map.getUnderlyingValue, staticContext)
          for ((key,value) <- sermap.asImmutableMap().asScala) {
            val name = key.getQNameValue
            _serialization(name) = value.getUnderlyingValue.getStringValue
          }
        case _ =>
          throw XProcException.xdValueDoesNotSatisfyType(value.item.toString, location)
      }
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    if (parent.isDefined) {
      parent.get match {
        case cont: XContainer =>
          val steps = cont.children[XStep]
          if (steps.nonEmpty) {
            for (child <- allChildren) {
              child.elaborateDefaultReadablePort(steps.last.primaryOutput)
            }
            initial
          } else {
            super.elaborateDefaultReadablePort(initial)
          }
        case _ =>
          super.elaborateDefaultReadablePort(initial)
      }
    } else {
      super.elaborateDefaultReadablePort(initial)
    }
  }

  override def elaboratePortConnections(): Unit = {
    super.elaboratePortConnections()
    if (parent.isDefined) {
      parent.get match {
        case decl: XDeclareStep =>
          if (!decl.atomic) {
            checkOutputBinding(decl)
          }
        case cont: XContainer =>
          checkOutputBinding(cont)
        case _ =>
          throw XProcException.xiThisCantHappen("p:output is not a child of a container?")
      }
    } else {
      throw XProcException.xiThisCantHappen("p:output has no parent?")
    }
  }

  private def checkOutputBinding(cont: XContainer): Unit = {
    if (children[XDataSource].isEmpty && primary) {
      val last = cont.children[XStep].last
      if (last.primaryOutput.isDefined) {
        val pipe = new XPipe(this, Some(last.stepName), Some(last.primaryOutput.get.port))
        addChild(pipe)
      }
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("port", Some(_port))
    attr.put("select", _select)
    attr.put("primary", _primary)
    attr.put("sequence", _sequence)
    dumpTree(sb, "p:output", attr.toMap)
  }
}
