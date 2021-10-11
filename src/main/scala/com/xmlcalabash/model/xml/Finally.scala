package com.xmlcalabash.model.xml

import com.jafpl.graph.{Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class Finally(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val input = new DeclareInput(config)
    input.port = "error"
    input.sequence = true
    input.primary = true
    addChild(input, firstChild)

    makeContainerStructureExplicit()

    for (output <- children[DeclareOutput]) {
      if (output.primary) {
        throw XProcException.xsPrimaryOutputOnFinally(output.port, location)
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[TryCatchStart]
    val node = start.addFinally(stepName, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, _graphNode.get)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startFinally(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endFinally()
  }

  override def toString: String = {
    s"p:finally $stepName"
  }
}