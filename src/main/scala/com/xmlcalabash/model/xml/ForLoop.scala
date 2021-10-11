package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

class ForLoop(override val config: XMLCalabashConfig) extends ForContainer(config) with NamedArtifact {
  private val _from = new QName("from")
  private val _to = new QName("to")
  private val _by = new QName("by")
  private var countFrom = 1L
  private var countTo = 0L
  private var countBy = 1L

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(_from)) {
      countFrom = attr(_from).get.toLong
    }

    if (attributes.contains(_to)) {
      countTo = attr(_to).get.toLong
    } else {
      throw new RuntimeException("to is required")
    }

    if (attributes.contains(_by)) {
      countBy = attr(_by).get.toLong
    }

    if (countBy == 0) {
      throw XProcException.xiAttempToCountByZero(location)
    }

    if ((countFrom > countTo && countBy > 0)
      || (countFrom < countTo && countBy < 0)) {
      logger.debug(s"Counting from $countFrom to $countTo by $countBy will never execute")
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    setupLoopInputs(None)
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addFor(stepName, countFrom, countTo, countBy, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
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

    for (child <- children[Step]) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startForLoop(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endForLoop()
  }

  override def toString: String = {
    s"cx:until $stepName"
  }
}