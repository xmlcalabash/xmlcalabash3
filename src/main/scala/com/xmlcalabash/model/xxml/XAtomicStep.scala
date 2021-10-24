package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StepExecutable, StepProxy, StepRunner, StepWrapper, XMLCalabashRuntime, XmlStep}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XAtomicStep(config: XMLCalabash, val stepType: QName) extends XStep(config) {
  _type = Some(stepType)

  def this(parentStep: XContainer, stepType: QName) = {
    this(parentStep.config, stepType)
    staticContext = parentStep.staticContext
    parent = parentStep
    _synthetic = true
    _syntheticName = Some(stepType)
  }

  override def primaryOutput: Option[XPort] = {
    children[XWithOutput] find {
      _.primary
    }
  }

  // =======================================================================================
  // =======================================================================================
  // =======================================================================================
  // =======================================================================================

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    if (ancestor[XDeclContainer].get.findDeclaration(stepType).isEmpty) {
      return
    }

    val decl = stepDeclaration.get

    val seenPorts = mutable.HashSet.empty[String]
    val seenOptions = mutable.HashSet.empty[QName]
    val newChildren = ListBuffer.empty[XArtifact]

    for (input <- children[XWithInput]) {
      input.validate()
      newChildren += input
      if (!input.portSpecified) {
        val idecl = decl.children[XInput] find { _.primary }
        if (idecl.isDefined) {
          input.port = idecl.get.port
        } else {
          error(XProcException.xsNoPrimaryInputPort(stepType, location))
          return
        }
      }

      if (seenPorts.contains(input.port)) {
        input.error(XProcException.xsDupWithInputPort(input.port, None))
      } else {
        if (decl.inputPorts.contains(input.port)) {
          seenPorts += input.port
        } else {
          input.error(XProcException.xsNoSuchPort(input.port, stepType, None))
        }
      }
    }

    for (input <- decl.children[XInput]) {
      if (!seenPorts.contains(input.port)) {
        seenPorts += input.port
        val xwi = new XWithInput(this, input.port)
        newChildren += xwi
        xwi.validate()
      }
    }

    // Configure the XWithInput like it's XInput declaration
    for (input <- children[XWithInput]) {
      val idecl = decl.children[XInput] find { _.port == input.port }
      input.sequence = idecl.get.sequence
      input.contentTypes = idecl.get.contentTypes
      input.primary = idecl.get.primary
    }

      if (children[XWithOption].nonEmpty) {
      val input = new XWithInput(this, "#bindings")
      input.primary = false
      input.sequence = true
      input.contentTypes = MediaType.MATCH_ANY
      input.validate()
      newChildren += input
    }

    for (output <- decl.outputs) {
      seenPorts += output.port
      val xwo = new XWithOutput(this, output.port)
      xwo.validate()
      newChildren += xwo
    }

    for (option <- children[XWithOption]) {
      option.validate()
      if (seenOptions.contains(option.name)) {
        option.error(XProcException.xsDupWithOptionName(option.name, None))
      } else {
        seenOptions += option.name
      }
      newChildren += option
    }

    for (opt <- decl.options) {
      if (!seenOptions.contains(opt.name)) {
        if (opt.required) {
          error(XProcException.xsMissingRequiredOption(opt.name, location))
        }
        val expr = opt.select.getOrElse("()")
        val xwo = new XWithOption(this, opt.name, None, Some(expr))
        newChildren += xwo
      }
    }

    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XWithInput => ()
        case _: XWithOutput => ()
        case _: XWithOption => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = newChildren.toList
  }

  // =======================================================================================
  // =======================================================================================
  // =======================================================================================
  // =======================================================================================

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    val decl = stepDeclaration
    if (decl.isEmpty) {
      if (stepType.getNamespaceURI == XProcConstants.ns_p) {
        error(XProcException.xsElementNotAllowed(stepType, location))
      } else {
        error(XProcException.xsMissingDeclaration(stepType, location))
      }
      return
    }

    for (name <- attributes.keySet) {
      if (decl.get.option(name).isDefined) {
        syntheticOption(name, attr(name).get)
      } else {
        error(XProcException.xsUndeclaredOption(stepType, name, location))
      }
    }
  }

  /*
  override def elaborateDeclarations(inScopeNames: Set[String]): Unit = {
    val seenPorts = mutable.HashSet.empty[String]
    val seenOptions = mutable.HashSet.empty[QName]
    val newChildren = ListBuffer.empty[XArtifact]

    val skeleton = declarationContainer.findSkeleton(stepType).get

    for (input <- children[XWithInput]) {
      if (!input.portSpecified) {
        val idecl = skeleton.decl.children[XInput] find {
          _.primary
        }
        if (idecl.isDefined) {
          input.port = idecl.get.port
        } else {
          error(XProcException.xsNoPrimaryInputPort(stepType, location))
          return
        }
      }
      if (seenPorts.contains(input.port)) {
        input.error(XProcException.xsDupWithInputPort(input.port, None))
        newChildren += input
      } else {
        if (skeleton.inputs.contains(input.port)) {
          seenPorts += input.port
          input.elaborateDeclarations()
          newChildren += input
        } else {
          input.error(XProcException.xsNoSuchPort(input.port, stepType, None))
          newChildren += input // make sure we don't loose the exception!
        }
      }
    }

    for (port <- skeleton.inputs) {
      if (!seenPorts.contains(port)) {
        seenPorts += port
        val input = new XWithInput(this, port)
        newChildren += input
        input.elaborateDeclarations()
      }
    }

    if (children[XWithOption].nonEmpty) {
      val input = new XWithInput(this, "#bindings")
      input.primary = false
      input.sequence = true
      input.contentTypes = MediaType.MATCH_ANY
      input.elaborateDeclarations()
      newChildren += input
    }

    for (port <- skeleton.outputs) {
      seenPorts += port
      val output = new XWithOutput(this, port)
      output.elaborateDeclarations()
      newChildren += output
    }

    for (option <- children[XWithOption]) {
      if (seenOptions.contains(option.name)) {
        option.error(XProcException.xsDupWithOptionName(option.name, None))
      } else {
        seenOptions += option.name
        option.elaborateDeclarations()
      }
      newChildren += option
    }

    for (name <- skeleton.options) {
      if (!seenOptions.contains(name)) {
        val odecl = skeleton.decl.option(name).get
        if (odecl.required) {
          throw XProcException.xsMissingRequiredOption(name, location)
        }
        val expr = odecl.select.getOrElse("()")
        val opt = new XWithOption(this, name, None, Some(expr))
        newChildren += opt
      }
    }

    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XWithInput => ()
        case _: XWithOption => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = newChildren.toList
  }
*/

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    super.elaborateDefaultReadablePort(initial)
    val drp = children[XWithOutput] find {
      _.primary
    }
    drp
  }

  override protected[xxml] def elaborateValidatePortConnections(ports: XPortBindingContext): Unit = {
    super.elaborateValidatePortConnections(ports)

    val decl = stepDeclaration.get

    for (child <- children[XWithInput]) {
      if (!child.port.startsWith("#depends_") && child.port != "#bindings") {
        if (child.children[XDataSource].isEmpty) {
          val input = decl.children[XInput] find { _.port == child.port }
          if (input.get.defaultInputs.nonEmpty) {
            child.allChildren = input.get.defaultInputs
          } else {
            error(XProcException.xsUnconnectedInputPort(stepName, child.port, location))
          }
        }
      }
    }
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    var bcontext = initial
    for (child <- allChildren) {
      bcontext = child.elaborateNameBindings(bcontext)
    }

    staticContext = staticContext.withConstants(bcontext)

    initial
  }

  override protected def elaborateDynamicOptions(): Unit = {
    children[XWithOption] foreach { _.elaborateDynamicOptions() }
    val xbind = children[XWithInput] find { _.port == "#bindings" }
    if (xbind.isDefined) {
      if (xbind.get.allChildren.isEmpty) {
        removeChild(xbind.get)
      }
    }
  }

  // =======================================================================================

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    impl.configure(config, stepType, name, None)

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, stepType.toString))
  }

  protected def stepImplementation: StepExecutable = {
    val decl = stepDeclaration
    if (decl.isEmpty) {
      throw XProcException.xsMissingDeclaration(stepType, location)
    }
    if (decl.get.atomic) {
      if (decl.get.implementationClass.isDefined) {
        val implClass = decl.get.implementationClass.get
        val klass = Class.forName(implClass).getDeclaredConstructor().newInstance()
        klass match {
          case step: XmlStep =>
            new StepWrapper(step, decl.get)
          case _ =>
            throw XProcException.xiStepImplementationError(s"Class does not implement an XmlStep: ${stepType}", location)
        }
      } else {
        throw XProcException.xiStepImplementationError(s"No implementation for ${stepType}", location);
      }
    } else {
      new StepRunner(decl.get)
    }
  }

  // =======================================================================================

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("type", Some(stepType.getEQName))
    attr.put("name", Some(stepName))
    dumpTree(sb, "p:atomic-step", attr.toMap)
  }

  override def toString: String = {
    if (stepName != tumble_id) {
      s"${stepType}(${stepName};${tumble_id})"
    } else {
      s"${stepType}(${stepName})"
    }
  }
}
