package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants, XValueParser}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.steps.internal.ValueComputation
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.XdmAtomicValue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XVariable(config: XMLCalabash) extends XNameBinding(config) with XGraphableArtifact {
  private var _allowedValues = Option.empty[List[XdmAtomicValue]]

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    if (_select.isEmpty) {
      error(XProcException.xsMissingRequiredAttribute(XProcConstants._select, location))
    }

    _allowedValues = checkValueTokens
    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    val inputs = validateExplicitConnections(_href, _pipe)
    _href = None
    _pipe = None
    val newChildren = ListBuffer.empty[XArtifact]

    if (inputs.nonEmpty) {
      val xwi = new XWithInput(this, "source")
      xwi.primary = true
      xwi.sequence = false
      xwi.contentTypes = MediaType.MATCH_ANY
      newChildren += xwi
      xwi.allChildren = inputs
    }

    val xwo = new XWithOutput(this, "result")
    xwo.primary = true
    xwo.sequence = true
    xwo.contentTypes = MediaType.MATCH_ANY
    newChildren += xwo

    allChildren = newChildren.toList
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    val bcontext = super.elaborateNameBindings(initial)

    var newContext = bcontext
    try {
      newContext = bcontext.withBinding(this)
    } catch {
      case ex: Exception =>
        error(ex)
    }

    newContext
  }

  override protected def promotedStaticValue(staticValueMsg: XdmValueItemMessage): XdmValueItemMessage = {
    XNameBinding.promotedValue(config, name, declaredType, None, staticValueMsg)
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (constantValue.isDefined || !usedByPipeline) {
      return
    }

    val start = parent.asInstanceOf[ContainerStart]
    val params = new XPathBindingParams(collection)
    val init = new XProcXPathExpression(staticContext, _select.getOrElse("()"), _declaredType, _allowedValues, params)
    // FIXME: does params here have to include all the statics?
    runtime.addNode(this, start.addOption(_name.getClarkName, init, params))
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

    attr.put("as", as)
    attr.put("required", _required)
    attr.put("visiblity", _visibility)

    if (drp.isDefined) {
      attr.put("drp", Some(drp.get.tumble_id))
    }

    dumpTree(sb, "p:variable", attr.toMap)
  }

  override def toString: String = {
    if (constantValue.isDefined) {
      s"${name}: ${constantValue.get.item} (constant)"
    } else if (_avt.isDefined) {
      s"${name}: ${_avt.get} (avt)"
    } else {
      if (_select.isDefined) {
        s"${name}: ${_select.get} (select)"
      } else {
        s"${name}: ???"
      }
    }
  }
}
