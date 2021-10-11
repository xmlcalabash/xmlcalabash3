package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class ForEach(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val fc = firstChild
    if (firstWithInput.isEmpty) {
      val winput = new WithInput(config)
      winput.port = "source"
      winput.primary = true
      winput.sequence = true
      addChild(winput, fc)
    } else {
      val wi = firstWithInput.get
      wi.primary = true
      wi.sequence = true
      if (wi.port == "") {
        wi.port = "source"
      }
    }

    val input = new DeclareInput(config)
    input.port = "current"
    input.primary = true
    input.contentTypes = List(MediaType.OCTET_STREAM)
    addChild(input, fc)

    makeContainerStructureExplicit()
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addForEach(stepName, containerManifold)
    _graphNode = Some(node)

    for (child <- allChildren) {
      child match {
        case _: Step =>
          child.graphNodes(runtime, node)
        case _: Variable =>
          child.graphNodes(runtime, node)
        case _ => ()
      }
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    val winput = firstWithInput
    if (winput.isDefined) {
      for (pipe <- winput.get.children[Pipe]) {
        runtime.graph.addOrderedEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, "source")
      }
    }

    for (output <- children[DeclareOutput]) {
      for (pipe <- output.children[Pipe]) {
        runtime.graph.addOrderedEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, output.port)
      }
    }

    for (child <- allChildren) {
      child match {
        case _: Step =>
          child.graphEdges(runtime, _graphNode.get)
        case _: Variable =>
          child.graphEdges(runtime, _graphNode.get)
        case _ => ()
      }
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startForEach(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endForEach()
  }

  override def toString: String = {
    s"p:for-each $stepName"
  }
}