package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XChoose(config: XMLCalabash) extends XContainer(config) {

  def this(parentStep: XArtifact) = {
    this(parentStep.config)
    staticContext = parentStep.staticContext
    parent = parentStep
    synthetic = true
    syntheticName = XProcConstants.p_choose
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    var seenWithInput = false
    var seenWhen = false;
    var seenOtherwise = false

    //val newScope = checkStepNameScoping(inScopeNames)

    val newChildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XOutput =>()
        case input: XWithInput =>
          if (seenWithInput) {
            error(XProcException.xsInvalidPipeline("A p:choose can have at most one p:with-input", location))
          }
          if (seenWhen || seenOtherwise) {
            error(XProcException.xsInvalidPipeline("A p:with-input cannot follow p:when or p:otherwise in p:choose", location))
          }
          seenWithInput = true
          newChildren += input
        case when: XWhen =>
          if (seenOtherwise) {
            error(XProcException.xsInvalidPipeline("A p:when cannot follow p:otherwise in p:choose", location))
          }
          seenWhen = true
          newChildren += when
        case otherwise: XOtherwise =>
          if (seenOtherwise) {
            error(XProcException.xsInvalidPipeline("A p:choose can have at most one p:otherwise", location))
          }
          seenOtherwise = true
          newChildren += otherwise
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    if (!seenWhen && !seenOtherwise) {
      error(XProcException.xsMissingWhen(None))
    }

    allChildren = newChildren.toList

    if (exceptions.nonEmpty) {
      return
    }

    val branch = children[XChooseBranch].head
    if (children[XOtherwise].isEmpty) {
      val other = new XOtherwise(this)
      addChild(other)

      val identity = new XAtomicStep(other, XProcConstants.p_identity)
      other.addChild(identity)

      val wi = new XWithInput(identity, "source")
      identity.addChild(wi)

      if (branch.primaryOutput.isEmpty) {
        val sink = new XAtomicStep(other, XProcConstants.p_sink)
        other.addChild(sink)
      } else {
        val xout = new XOutput(other, Some(branch.primaryOutput.get.port))
        xout.primary = true
        xout.sequence = true
        other.insertBefore(xout, identity)
      }

      other.validate()
    }

    // Make sure all of the branches have consistent primary output ports
    var cprimary = Option.empty[String]
    for (branch <- children[XChooseBranch]) {
      for (output <- branch.children[XOutput]) {
        if (output.primary) {
          cprimary = Some(output.port)
        }
      }
    }

    val boutputs = mutable.HashMap.empty[String, XOutput]
    for (branch <- children[XChooseBranch]) {
      var bprimary = Option.empty[String]
      for (output <- branch.children[XOutput]) {
        if (cprimary.isDefined && output.primary && cprimary.get != output.port) {
          error(XProcException.xsBadChooseOutputs(output.port, cprimary.get, location))
        } else {
          if (boutputs.contains(output.port)) {
            if (boutputs(output.port).primary != output.primary) {
              error(XProcException.xsBadChooseOutputs(output.port, location))
            }
          } else {
            boutputs.put(output.port, output)
          }
        }
        if (output.primary) {
          bprimary = Some(output.port)
        }
      }
      if (cprimary.isDefined != bprimary.isDefined) {
        if (cprimary.isDefined) {
          error(XProcException.xsBadChooseOutputs(cprimary.get, location))
        } else {
          error(XProcException.xsBadChooseOutputs(bprimary.get, location))
        }
      }
    }

    for (port <- boutputs.keySet) {
      val oport = if (port == "") {
        None
      } else {
        Some(port)
      }
      val output = new XOutput(this, oport)
      output.primary = boutputs(port).primary
      output.sequence = true
      output.contentTypes = MediaType.MATCH_ANY
      insertBefore(output, allChildren.head)

      for (branch <- children[XChooseBranch]) {
        val out = branch.children[XOutput] find { _.port == port }
        if (out.isDefined) {
          val pipe = new XPipe(output, branch.stepName, port)
          output.addChild(pipe)
        }
      }
    }
  }

  override def elaboratePortConnections(): Unit = {
    if (_drp.isDefined && children[XWithInput].isEmpty) {
      val input = new XWithInput(config)
      input.staticContext = staticContext
      input.parent = this
      val pipe = new XPipe(_drp.get)
      pipe.parent = input
      input.addChild(pipe)
      insertBefore(input, allChildren.head)
    }

    super.elaboratePortConnections()

    // The with-input isn't needed anymore
    val input = children[XWithInput].headOption
    if (input.isDefined) {
      input.get.irrelevant = true
    }
  }

  override def publicPipeConnections: Map[String,XPort] = {
    Map()
  }

  override def privatePipeConnections: Map[String,XPort] = {
    val ports = mutable.HashMap.empty[String,XPort]

    for (input <- children[XInput]) {
      val port = input.port
      ports.put(s"${name}/${port}", input)
    }

    for (child <- children[XStep]) {
      val name = child.stepName
      for (output <- child.children[XOutput]) {
        val port = output.port
        ports.put(s"${name}/${port}", output)
      }
      for (output <- child.children[XWithOutput]) {
        val port = output.port
        ports.put(s"${name}/${port}", output)
      }
    }

    // There are no option or variable children of p:choose

    ports.toMap
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addChoose(stepName)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
