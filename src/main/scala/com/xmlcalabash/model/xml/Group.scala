package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node, TryCatchStart}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class Group(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    makeContainerStructureExplicit()
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    if (parent.get.isInstanceOf[Try]) {
      val start = parNode.asInstanceOf[TryCatchStart]
      val node = start.addTry(stepName, containerManifold)
      _graphNode = Some(node)
    } else {
      val start = parNode.asInstanceOf[ContainerStart]

      val node = start.addGroup(stepName, containerManifold)
      _graphNode = Some(node)
    }

    for (child <- children[Step]) {
      child.graphNodes(runtime, _graphNode.get)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for (output <- children[DeclareOutput]) {
      for (pipe <- output.children[Pipe]) {
        runtime.graph.addOrderedEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, output.port)
      }
    }

    for (child <- children[Step]) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startGroup(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endGroup()
  }

  override def toString: String = {
    s"p:group $stepName"
  }
}