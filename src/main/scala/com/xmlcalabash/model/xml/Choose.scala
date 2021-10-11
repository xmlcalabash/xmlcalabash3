package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node, WhenStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class Choose(override val config: XMLCalabashConfig) extends Container(config) {
  private var hasWhen = false
  private var hasOtherwise = false
  protected var ifexpr: Option[String] = None
  protected var ifcoll: Option[String] = None
  protected[xml] var p_if = false

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (node.getNodeName == XProcConstants.p_if) {
      if (attributes.contains(XProcConstants._test)) {
        ifexpr = attr(XProcConstants._test)
      } else {
        throw XProcException.xsMissingRequiredAttribute(XProcConstants._test, location)
      }
      if (attributes.contains(XProcConstants._collection)) {
        ifcoll = attr(XProcConstants._collection)
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    var firstChild = Option.empty[Artifact]
    var input = Option.empty[WithInput]
    for (child <- allChildren) {
      if (firstChild.isEmpty) {
        firstChild = Some(child)
      }
      child match {
        case winput: WithInput =>
          if (input.isDefined) {
            throw new RuntimeException("Only one with-input is allowed")
          }
          input = Some(winput)
          winput.makeStructureExplicit()
        case when: When =>
          hasWhen = true
          when.makeStructureExplicit()
        case otherwise: Otherwise =>
          hasOtherwise = true
          otherwise.makeStructureExplicit()
      }
    }

    if (!hasWhen && !hasOtherwise) {
      throw XProcException.xsMissingWhen(location)
    }

    if (input.isEmpty) {
      val winput = new WithInput(config)
      winput.port = "source"
      winput.primary = true
      if (firstChild.isDefined) {
        addChild(winput, firstChild.get)
      } else {
        addChild(winput)
      }
    }

    val outputSet = mutable.HashSet.empty[String]
    var primaryOutput = Option.empty[String]

    for (branch <- children[Container]) {
      for (child <- branch.children[DeclareOutput]) {
        outputSet += child.port
        if (child.primary) {
          primaryOutput = Some(child.port)
        }
      }
    }

    val first = firstChild
    for (port <- outputSet) {
      val woutput = new WithOutput(config)
      woutput.port = port
      woutput.primary = (primaryOutput.isDefined && primaryOutput.get == port)
      addChild(woutput, first)
    }

    if (!hasOtherwise) {
      val other = new Otherwise(config)
      other.test = "true()"

      val identity = new AtomicStep(config)
      identity.stepType = XProcConstants.p_identity
      /* ??? tangled up in the collectin/expression debacle and unclear to me
      val idin = new WithInput(config)
      idin.port = "source"
      identity.addChild(idin)
       */
      other.addChild(identity)

      // If there isn't a primary output, make sure we sink the output of
      // the identity step we just added so that one doesn't get added
      // automatically
      if (primaryOutput.isEmpty) {
        val sink = new AtomicStep(config)
        sink.stepType = XProcConstants.p_sink
        other.addChild(sink)
      }

      addChild(other)
      other.makeStructureExplicit()
    }

    if (p_if) {
      var primary = false
      for (child <- children[WithOutput]) {
        primary = primary || child.primary
      }

      if (!primary) {
        throw XProcException.xsPrimaryOutputRequired(location)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    super.validateStructure()

    var first = true
    var primaryOutput = Option.empty[DeclareOutput]
    for (child <- allChildren) {
      child match {
        case _: WithInput => ()
        case _: WithOutput => ()
        case when: ChooseBranch =>  // When or Otherwise
          if (first) {
            for (child <- when.children[DeclareOutput]) {
              if (child.primary) {
                primaryOutput = Some(child)
              }
            }
            first = false
          }

          var foundPrimary = false
          for (child <- when.children[DeclareOutput]) {
            if (child.primary) {
              foundPrimary = true
              if (primaryOutput.isEmpty) {
                throw XProcException.xsBadChooseOutputs("#NONE", child.port, location)
              }
              if (primaryOutput.isDefined && primaryOutput.get.port != child.port) {
                throw XProcException.xsBadChooseOutputs(primaryOutput.get.port, child.port, location)
              }
            }
          }

          if (!foundPrimary && primaryOutput.isDefined) {
            throw XProcException.xsBadChooseOutputs(primaryOutput.get.port, "#NONE", location)
          }
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }
  }

  /*
  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()
    println("Hello")
  }
   */

    override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addChoose(stepName)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for (child <- children[Container]) {
      child.graphEdges(runtime, _graphNode.get)
    }

    val chooseNode = _graphNode.get.asInstanceOf[ChooseStart]
    for (branch <- children[ChooseBranch]) {
      val branchNode = branch._graphNode.get
      for (output <- branch.children[DeclareOutput]) {
        runtime.graph.addEdge(branchNode, output.port, chooseNode, output.port)
      }
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startChoose(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endChoose()
  }

  override def toString: String = {
    s"p:choose $stepName"
  }
}