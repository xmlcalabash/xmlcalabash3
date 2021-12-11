package com.xmlcalabash.model.xxml

import com.jafpl.graph.{Node, TryCatchStart}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime

class XFinally(config: XMLCalabash) extends XTryCatchBranch(config) {

  override protected[xxml] def validate(): Unit = {
    super.validate()

    val primary = children[XOutput] find { _.primary == true }
    if (primary.isDefined) {
      error(XProcException.xsPrimaryOutputOnFinally(primary.get.port, location))
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[TryCatchStart]
    val node = start.addFinally(stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }

}