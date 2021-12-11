package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node, TryCatchStart}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime

class XGroup(config: XMLCalabash) extends XContainer(config) {
  def this(parent: XContainer) = {
    this(parent.config)
    this.parent = parent
    staticContext = parent.staticContext
    _synthetic = true
    _syntheticName = Some(XProcConstants.p_group)
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    val node = if (parent.get.isInstanceOf[XTry]) {
      val start = parNode.asInstanceOf[TryCatchStart]
      start.addTry(stepName, containerManifold)
    } else {
      val start = parNode.asInstanceOf[ContainerStart]
      start.addGroup(stepName, containerManifold)
    }
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
