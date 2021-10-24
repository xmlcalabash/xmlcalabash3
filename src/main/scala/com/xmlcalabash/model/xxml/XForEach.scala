package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.runtime.XMLCalabashRuntime

class XForEach(config: XMLCalabash) extends XLoopingStep(config) {

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addForEach(stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
