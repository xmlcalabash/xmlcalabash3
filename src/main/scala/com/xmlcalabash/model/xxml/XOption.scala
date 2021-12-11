package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{TypeUtils, XProcVarValue}
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmAtomicValue}
import net.sf.saxon.trans.XPathException

import scala.collection.mutable

class XOption(config: XMLCalabash) extends XNameBinding(config) with XGraphableArtifact {
  private var _allowedValues = Option.empty[List[XdmAtomicValue]]
  private var _runtimeBindings = Map.empty[QName, XProcVarValue]
  private var _cx_as = Option.empty[String]

  def this(config: XMLCalabash, name: QName, value: XdmValueItemMessage) = {
    this(config)
    _name = name
    _constantValue = Some(value)
    _synthetic = true
    _constant = true
    staticContext = new XArtifactContext(this, value.context, XProcConstants.p_option)
  }

  def tokenList: Option[List[XdmAtomicValue]] = _allowedValues

  def allowedValues: Option[List[XdmAtomicValue]] = _allowedValues

  override def usedByPipeline: Boolean = {
    // always evaluate these so we find static errors
    true
  }

  def runtimeBindings(bindings: Map[QName, XProcVarValue]): Unit = {
    _runtimeBindings = bindings
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    _cx_as = attr(XProcConstants.cx_as)
    _allowedValues = checkValueTokens
  }

  override protected[xxml] def validate(): Unit = {
    if (!static) {
      parent.get match {
        case _: XLibrary =>
          error(XProcException.xsOptionMustBeStatic(name, None))
        case _ => ()
      }
    }

    for (child <- allChildren) {
      child match {
        case _: XPipeinfo =>
        case _: XDocumentation =>
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = List()

    val xwo = new XWithOutput(this, "result")
    addChild(xwo)
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    super.elaborateNameBindings(initial)

    var newContext = initial
    if (static) {
      _constant = true
      _constantValue = Some(config.staticOptions(name))
      newContext = initial.withBinding((this))
    } else {
      newContext = initial.withBinding(this)
      if (select.isDefined) {
        val compiler = config.processor.newXPathCompiler()
        for (name <- _variableReferences) {
          compiler.declareVariable(name)
        }
        for ((prefix,uri) <- staticContext.inscopeNamespaces) {
          compiler.declareNamespace(prefix, uri)
        }
        try {
          compiler.compile(select.get)
        } catch {
              /*
          case ex: SaxonApiException =>
            throw XProcException.xsStaticErrorInExpression(select.get, ex.getMessage, location)
          case ex: Exception =>
            throw ex
               */
          case ex: Exception =>
            logger.debug(ex.getMessage)
        }
      }
    }

    newContext
  }

  override protected[xxml] def elaboratePortConnections(): Unit = {
    // p:option doesn't have any port connections
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (!static && usedByPipeline) {
      val params = new XPathBindingParams(false)
      val expr = new XProcXPathExpression(staticContext, _select.getOrElse("()"), declaredType, _allowedValues, params)
      val start = parent.asInstanceOf[ContainerStart]
      runtime.addNode(this, start.addOption(name.getClarkName, expr, params, true))
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    if (Option(_name).isDefined) {
      attr.put("name", Some(_name.getEQName))
    }

    if (constantValue.isDefined) {
      attr.put("constant-value", Some(constantValue.get.item.toString))
    } else {
      attr.put("select", _select)
      attr.put("avt", _avt)
    }

    attr.put("as", _as)
    attr.put("required", _required)
    attr.put("visiblity", _visibility)
    dumpTree(sb, "p:option", attr.toMap)
  }
}
