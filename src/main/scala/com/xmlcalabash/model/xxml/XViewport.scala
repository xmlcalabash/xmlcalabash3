package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.{XMLViewportComposer, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime

class XViewport(config: XMLCalabash) extends XLoopingStep(config) {
  private var _match: String = _

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    if (attributes.contains(XProcConstants._match)) {
      _match = attr(XProcConstants._match).get
    } else {
      throw new RuntimeException("Viewport must have match")
    }
  }

  override protected[xxml] def validate(): Unit = {
    super.validate()

    var found = false
    for (child <- children[XOutput]) {
      if (found) {
        throw new RuntimeException("Viewport must not have more than one output")
      }
      found = true
      child.port = "result"
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val composer = new XMLViewportComposer(config, staticContext, _match)
    val node = start.addViewport(composer, stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
