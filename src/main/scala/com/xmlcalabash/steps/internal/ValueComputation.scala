package com.xmlcalabash.steps.internal

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.util.XProcConstants.ValueTemplate
import com.xmlcalabash.model.xxml.{XArtifact, XAtomicStep}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, SequenceType, XdmAtomicValue}

class ValueComputation private(parentStep: XArtifact, name: QName, collection: Boolean) extends XAtomicStep(parentStep.config, XProcConstants.cx_value_computation) {
  private var _avt = Option.empty[ValueTemplate]
  private var _select = Option.empty[String]
  private var _as = Option.empty[SequenceType]
  private var _tokens = Option.empty[List[XdmAtomicValue]]
  staticContext = parentStep.staticContext
  _synthetic = true
  parent = parentStep

  def this(parentStep: XArtifact, name: QName, avt: ValueTemplate, collection: Boolean) = {
    this(parentStep, name, collection)
    _avt = Some(avt)
  }

  def this(parentStep: XArtifact, name: QName, select: String, collection: Boolean) = {
    this(parentStep, name, collection)
    _select = Some(select)
  }

  def valueName: QName = name
  def avt: Option[ValueTemplate] = _avt
  def select: Option[String] = _select

  def as: Option[SequenceType] = _as
  protected[xmlcalabash] def as_=(seqType: SequenceType): Unit = {
    _as = Some(seqType)
  }

  def allowedValues: Option[List[XdmAtomicValue]] = _tokens
  protected[xmlcalabash] def allowedValues_=(values: List[XdmAtomicValue]): Unit = {
    _tokens = Some(values)
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val init = if (_avt.isDefined) {
      new XProcVtExpression(staticContext, _avt.get, true)
    } else {
      val params = new XPathBindingParams(staticContext.inscopeConstantValues, collection)
      new XProcXPathExpression(staticContext, _select.getOrElse("()"), as, _tokens, Some(params))
    }

    val cnode = runtime.node(ancestorContainer.get).asInstanceOf[ContainerStart]
    runtime.addNode(this, cnode.addOption(name.getClarkName, init))
  }
}
