package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{ExprParams, XMLCalabashRuntime, XProcXPathExpression}
import com.xmlcalabash.util.xc.ElaboratedPipeline

class Variable(override val config: XMLCalabashConfig) extends NameBinding(config) {

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (static) {
      // Statics have already been evaluated, they don't appear in the graph
      return
    }

    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    /*
    if (cnode.parent.nonEmpty) {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }
     */

    val params = new XPathBindingParams(collection)
    val init = new XProcXPathExpression(staticContext, _select.getOrElse("()"), as, _allowedValues, params)
    val node = cnode.addOption(_name.getClarkName, init, xpathBindingParams())
    _graphNode = Some(node)
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    if (static) {
      return
    }

    xml.startVariable(tumble_id, tumble_id, name)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWithVariable()
  }

  override def toString: String = {
    s"p:variable $name $tumble_id"
  }
}
