package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.XmlItemTester
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

class ForWhile(override val config: XMLCalabashConfig) extends ForContainer(config) with NamedArtifact {
  private val _max_iterations = new QName("max-iterations")
  private val _return = new QName("return")
  private var maxIterations: Long = -1
  private var test: String = ""
  private var returnSet: String = "last"

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(_max_iterations)) {
      maxIterations = attr(_max_iterations).get.toInt
    }

    if (attributes.contains(XProcConstants._test)) {
      test = attr(XProcConstants._test).get
    } else {
      throw new RuntimeException("test is required")
    }

    if (attributes.contains(_return)) {
      returnSet = attr(_return).get
      if (returnSet != "last" && returnSet != "all") {
        throw new RuntimeException("return must be last or all")
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    setupLoopInputs(Some(true))

    // Now let's consider what making the container structure explicit has done.
    // If either the primary output or the test output haven't been specified,
    // make sure it defaults to the primary output of the last step.
    // Note: there will always be at least one!
    var testOutput = Option.empty[DeclareOutput]
    var resultOutput = Option.empty[DeclareOutput]
    for (child <- children[DeclareOutput]) {
      if (child.port == "test") {
        testOutput = Some(child)
        child.primary = false
      } else {
        if (child.primary) {
          resultOutput = Some(child)
        }
      }
    }

    if (resultOutput.isEmpty) {
      setDefaultOutput("#result", true)
    }

    if (testOutput.isEmpty) {
      setDefaultOutput("test", false)
    }
  }

  private def setDefaultOutput(port: String, primary: Boolean): Unit = {
    var lastStep = Option.empty[Step]

    for (child <- allChildren) {
      child match {
        case atomic: AtomicStep => lastStep = Some(atomic)
        case compound: Container => lastStep = Some(compound)
        case _ => ()
      }
    }

    val output = new DeclareOutput(config)
    output.port = port
    output.primary = primary
    output.sequence = true

    val pipe = new Pipe(config)
    pipe.step = lastStep.get.stepName
    pipe.port = lastStep.get.primaryOutput.get.port
    pipe.link = lastStep.get.primaryOutput.get
    output.addChild(pipe)

    if (firstChild.isDefined) {
      addChild(output, firstChild.get)
    } else {
      addChild(output)
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val tester = new XmlItemTester(config, test, maxIterations, this)
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addWhile(tester, returnSet=="all", stepName, containerManifold)
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
    xml.startForWhile(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endForWhile()
  }

  override def toString: String = {
    s"cx:while $stepName"
  }
}