package com.xmlcalabash.model.xxml

import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XContainer(config: XMLCalabash) extends XStep(config) {
  protected var _drp = Option.empty[XPort]

  def containerManifold: Manifold = {
    val spec = mutable.HashMap.empty[String, PortCardinality]
    for (output <- outputs) {
      if (output.sequence) {
        spec.put(output.port, PortCardinality.ZERO_OR_MORE)
      } else {
        spec.put(output.port, PortCardinality.EXACTLY_ONE)
      }
    }
    new Manifold(Manifold.WILD, new PortSpecification(spec.toMap))
  }

  override def primaryOutput: Option[XPort] = {
    children[XOutput] find { _.primary }
  }

  protected def constructDefaultOutput(): Unit = {
    val firstStep = children[XStep].head
    val step = children[XStep].last
    val pout = step.primaryOutput
    if (pout.isEmpty) {
      return // There isn't a default putput
    }

    if (children[XOutput].nonEmpty) {
      val implicitPrimary = children[XOutput].length == 1
      for (xout <- children[XOutput]) {
        checkDefaultOutput(pout.get, xout, implicitPrimary)
      }
      return
    }

    val output = this match {
      case _: XViewport =>
        new XOutput(this, Some("result"))
      case _ =>
        new XOutput(this, None)
    }
    output.primary = true
    output.sequence = pout.get.sequence
    output.contentTypes = pout.get.contentTypes

    if (Option(firstStep).isDefined) {
      insertBefore(output, firstStep)
    } else {
      addChild(output)
    }

    checkDefaultOutput(pout.get, output, implicitPrimary=true)
  }

  private def checkDefaultOutput(stepOut: XPort, output: XOutput, implicitPrimary: Boolean): Unit = {
    if (implicitPrimary && !output.primarySpecified) {
      output.primary = true
    }

    if (!output.primary || output.children[XDataSource].nonEmpty) {
      return
    }

    val pipe = new XPipe(stepOut)
    output.addChild(pipe)
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    var curdrp  = initial
    for (child <- allChildren) {
      curdrp = child.elaborateDefaultReadablePort(curdrp)
    }

    val drp = children[XOutput] find { _.primary }
    drp
  }

  override protected[xxml] def elaborateDependsConnections(inScopeSteps: Map[String,XStep]): Unit = {
    for (name <- dependsOn.keySet) {
      if (inScopeSteps.contains(name)) {
        dependsOn.put(name, Some(inScopeSteps(name)))
      } else {
        error(XProcException.xsNotAStep(name, location))
      }
    }

    val steps = mutable.HashMap.empty[String,XStep] ++ inScopeSteps
    for (child <- children[XStep]) {
      child match {
        case _: XWhen => ()
        case _: XOtherwise => ()
        case _: XCatch => ()
        case _: XFinally => ()
        case _ =>
          if (child.name.isDefined) {
            steps.put(child.name.get, child)
          }
      }
    }

    for (child <- children[XStep]) {
      child.elaborateDependsConnections(steps.toMap)
    }
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    for (child <- allChildren) {
      child.validate()
    }

    val seenPorts = mutable.HashSet.empty[String]
    val newChildren = ListBuffer.empty[XArtifact]

    for (input <- children[XInput]) {
      if (seenPorts.contains(input.port)) {
        input.error(XProcException.xsDupPortName(input.port, None))
      } else {
        seenPorts += input.port
        newChildren += input
      }
    }

    for (output <- children[XOutput]) {
      if (seenPorts.contains(output.port)) {
        output.error(XProcException.xsDupPortName(output.port, None))
      } else {
        seenPorts += output.port
        newChildren += output
      }
    }

    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XInput => ()
        case _: XOutput => ()
        case _: XWithInput => ()
          if (this.isInstanceOf[XIf]) {
            newChildren += child
          } else {
            error(XProcException.xsElementNotAllowed(child.nodeName, None))
          }
        case _: XVariable =>
          newChildren += child
        case _: XStep =>
          newChildren += child
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = newChildren.toList

    if (children[XOutput].length == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }

    constructDefaultOutput()
  }

  protected def checkStepNameScoping(inScopeNames: Set[String]): Set[String] = {
    val newScope = mutable.Set.empty[String] ++ inScopeNames

    if (name.isDefined) {
      if (newScope.contains(name.get)) {
        error(XProcException.xsDuplicateStepName(name.get, location))
      }
      newScope += name.get
    }

    for (step <- children[XStep]) {
      if (step.name.isDefined) {
        if (newScope.contains(step.name.get)) {
          error(XProcException.xsDuplicateStepName(step.name.get, location))
        }
        newScope += step.name.get
      }
    }

    for (step <- children[XContainer]) {
      // newScope contains the names of all the children, but we need
      // to exclude *this* child's name for each descent.
      val childScope = mutable.Set.empty[String] ++ newScope
      if (step.name.isDefined) {
        childScope -= step.name.get
      }
      step.checkStepNameScoping(childScope.toSet)
    }

    newScope.toSet
  }

  def publicPipeConnections: Map[String,XPort] = {
    val ports = mutable.HashMap.empty[String,XPort]

    for (input <- children[XInput]) {
      val port = input.port
      ports.put(s"${name.getOrElse(tumble_id)}/${port}", input)
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

    for (child <- children[XOption]) {
      ports.put(s"${child.tumble_id}/result", child.children[XWithOutput].head)
    }
    for (child <- children[XVariable]) {
      ports.put(s"${child.tumble_id}/result", child.children[XWithOutput].head)
    }

    ports.toMap
  }

  def privatePipeConnections: Map[String,XPort] = {
    // Private pipe connections are ones that must be available
    // to the pipeline engine, but aren't exposed to the pipeline
    // author. For example, the pipeline author can't connect to the
    // output ports on a p:when, but the p:choose step must be able to!
    Map()
  }

  override protected[xxml] def elaborateValidatePortConnections(ports: XPortBindingContext): Unit = {
    super.elaborateValidatePortConnections(ports)
    if (exceptions.isEmpty) {
      this match {
        case _: XLibrary =>
          ()
        case decl: XDeclareStep =>
          if (!decl.atomic) {
            checkUnboundOutputs()
          }
        case _ =>
          checkUnboundOutputs()
      }
    }
  }

  private def checkUnboundOutputs(): Unit = {
    for (step <- children[XStep]) {
      for (io <- step.children[XPort]) {
        io match {
          case _: XInput =>
            ()
          case _: XWithInput =>
            ()
          case port: XPort =>
            ()
        }
      }
    }
  }

  override protected[xxml] def elaborateInsertContentTypeFilters(): Unit = {
    for (child <- children[XInput]) {
      if (!MediaType.OCTET_STREAM.allowed(child.contentTypes)) {
        val filterable = true // FIXME: not necessary for compound steps where we can see what the input is
        if (filterable) {
          addInputFilter(child, new XContentTypeChecker(this, child))
        }
      }
    }

    for (child <- children[XStep]) {
      child.elaborateInsertContentTypeFilters()
    }

    for (child <- children[XOutput]) {
      if (!MediaType.OCTET_STREAM.allowed(child.contentTypes)) {
        var filter = false
        for (pipe <- child.children[XPipe]) {
          for (ctype <- pipe.from.get.contentTypes filter { _.inclusive }) {
            filter = filter || !ctype.allowed(child.contentTypes)
          }
        }
        if (filter) {
          addOutputFilter(child, new XContentTypeChecker(this, child))
        }
      }
    }
  }

  override protected def addInputFilter(child: XPort, filter: XStep): Unit = {
    val firstChild = children[XStep].head
    val xwi = new XWithInput(filter, "source")
    xwi.primary = true
    xwi.sequence = child.sequence
    xwi.contentTypes = MediaType.MATCH_ANY.toList
    xwi.addChild(new XPipe(xwi, child))
    val xwo = new XWithOutput(filter, "result")
    xwo.primary = true
    xwo.sequence = child.sequence
    xwo.contentTypes = child.contentTypes
    filter.addChild(xwi)
    filter.addChild(xwo)
    patchBinding(this, child, xwo)
    insertBefore(filter, firstChild)
    xwi.validate()
    xwo.validate()
  }

  private def addOutputFilter(child: XPort, filter: XStep): Unit = {
    val xwi = new XWithInput(filter, "source")
    xwi.primary = true
    xwi.sequence = child.sequence
    xwi.contentTypes = MediaType.MATCH_ANY.toList
    xwi.allChildren = child.allChildren
    child.allChildren = List()

    val xwo = new XWithOutput(filter, "result")
    xwo.primary = true
    xwo.sequence = child.sequence
    xwo.contentTypes = child.contentTypes
    filter.addChild(xwi)
    filter.addChild(xwo)
    child.addChild(new XPipe(child, filter.stepName, "result"))
    addChild(filter)
    xwi.validate()
    xwo.validate()
  }

  private def patchBinding(artifact: XArtifact, from: XPort, originPort: XPort): Unit = {
    // Don't replace them while we're walking over the data structure
    val replace = ListBuffer.empty[XPipe]
    for (child <- artifact.allChildren) {
      child match {
        case pipe: XPipe =>
          if (pipe.from.isDefined && pipe.from.get == from) {
            replace += pipe
          }
        case _ =>
          patchBinding(child, from, originPort)
      }
    }
    for (pipe <- replace) {
      val newPipe = new XPipe(pipe.parent.get, originPort)
      pipe.parent.get.replaceChild(pipe, newPipe)
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("name", Some(stepName))
    dumpTree(sb, nodeName.toString, attr.toMap)
  }
}
