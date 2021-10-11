package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

class NamePipe(override val config: XMLCalabashConfig, val name: QName, val step: String, val link: NameBinding) extends Artifact(config) {
  private var _node = link._graphNode

  override def parse(node: XdmNode): Unit = {
    throw new RuntimeException("This is a purely synthetic element")
  }

  override protected[model] def validateStructure(): Unit = {
    if (allChildren.nonEmpty) {
      throw new RuntimeException(s"Invalid content in $this")
    }
  }

  // Name pipes for options that depend on the values of preceding options in the same
  // declaration get constructed before the corresponding with-option nodes for evaluating
  // them. This patch hack let's the node be updated later.
  protected[model] def patchNode(node: Node): Unit = {
    _node = Some(node)
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    if (_node.isEmpty) {
      _node = link._graphNode
    }

    if (_node.isEmpty) {
      throw XProcException.xiThisCantHappen(s"Attempt to link from non-existant graph node for ${link}", None)
    }

    val toNode = parNode
    val toPort = "#bindings"
    val fromNode = _node.get
    val fromPort = "result"
    runtime.graph.addEdge(fromNode, fromPort, toNode, toPort)
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startNamePipe(tumble_id, step)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endNamePipe()
  }

  override def toString: String = {
    s"p:name-pipe $name $step"
  }
}
