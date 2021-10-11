package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.StepParams
import com.xmlcalabash.runtime.{ImplParams, StepExecutable, StepProxy, StepRunner, StepWrapper, XMLCalabashRuntime, XmlStep}
import com.xmlcalabash.steps.internal.{DocumentLoader, InlineLoader}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.ma.arrays.ArrayItemType
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class AtomicStep(override val config: XMLCalabashConfig, params: Option[ImplParams]) extends Step(config) with NamedArtifact {
  def this(config: XMLCalabashConfig) = {
    this(config, None)
  }

  def this(config: XMLCalabashConfig, params: ImplParams) = {
    this(config, Some(params))
  }

  def this(config: XMLCalabashConfig, params: ImplParams, context: Artifact) = {
    this(config, Some(params))
    _inScopeStatics = context._inScopeStatics
    _inScopeDynamics = context._inScopeDynamics
  }

  private var _stepType: QName = _
  private var _stepImplementation: StepExecutable = _

  def stepType: QName = _stepType
  protected[model] def stepType_=(stype: QName): Unit = {
    _stepType = stype
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    _stepType = node.getNodeName
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    if (declaration(stepType).isEmpty) {
      throw XProcException.xsMissingDeclaration(stepType, location)
    }

    val decl = declaration(stepType).get

    for (item <- allChildren) {
      item match {
        case winput: WithInput =>
          winput.makeStructureExplicit()
          if (winput.port == "") {
            if (decl.primaryInput.isDefined) {
              winput.port = decl.primaryInput.get.port
            }
          }
        case _ =>
          item.makeStructureExplicit()
      }
    }

    // Make sure there are with-{input/output/option} elements
    for (dinput <- decl.inputs) {
      var found = Option.empty[WithInput]
      for (winput <- children[WithInput]) {
        if (dinput.port == winput.port) {
          found = Some(winput)
        }
      }
      if (found.isEmpty) {
        val newwi = new WithInput(config)
        newwi.port = dinput.port
        newwi.primary = dinput.primary
        newwi.sequence = dinput.sequence
        addChild(newwi)
      }
    }

    for (doutput <- decl.outputs) {
      val newwo = new WithOutput(config)
      newwo.port = doutput.port
      newwo.primary = doutput.primary
      newwo.sequence = doutput.sequence
      addChild(newwo)
    }

    for (doption <- decl.options) {
      var found = Option.empty[WithOption]
      for (woption <- children[WithOption]) {
        if (woption.name == doption.name) {
          found = Some(woption)
        }
      }

      if (found.isEmpty) {
        if (attributes.contains(doption.name)) {
          val woption = new WithOption(config, doption.name)
          woption.staticContext = staticContext

          if (doption.declaredType.isDefined) {
            val dtype = doption.declaredType.get
            woption.as = dtype
            woption.qnameKeys = doption.forceQNameKeys

            dtype.getItemType.getUnderlyingItemType match {
              case _: MapType =>
                woption.select = attr(doption.name).get
              case _: ArrayItemType =>
                woption.select = attr(doption.name).get
              case _ =>
                woption.avt = attr(doption.name).get
            }
          } else {
            woption.avt = attr(doption.name).get
          }

          found = Some(woption)
          addChild(woption)
        } else {
          if (doption.required) {
            throw XProcException.xsMissingRequiredOption(doption.name, location)
          } else {
            val woption = new WithOption(config, doption.name)
            woption.staticContext = staticContext
            woption.select = doption.defaultSelect.getOrElse("()")
            found = Some(woption)
            addChild(woption)
          }
        }
      } else {
        if (attributes.contains(doption.name)) {
          throw XProcException.xsDupWithOptionName(doption.name, location)
        }
      }

      // Work out any preceding options that might not be defined
      var prec = true
      for (precopt <- decl.options) {
        prec = prec && precopt.name != found.get.name
        if (prec) {
          found.get.precedingOption(precopt)
        }
      }

      if (doption.declaredType.isDefined) {
        found.get.declaredType = doption.declaredType.get
      }
    }

    // Make sure there are no extra options
    val seenOption = mutable.HashSet.empty[QName]
    for (woption <- children[WithOption]) {
      if (!decl.optionNames.contains(woption.name)) {
        throw XProcException.xsUndeclaredOption(stepType, woption.name, location)
      }
      if (seenOption.contains(woption.name)) {
        throw XProcException.xsDupWithOptionName(woption.name, location)
      }
      seenOption += woption.name
    }

    // Now manufacture "with-option"s for extension attributes
    for (attr <- extensionAttributes.keySet) {
      val woption = new WithOption(config, attr)
      woption.staticContext = staticContext
      woption.qnameKeys = false
      woption.avt = extensionAttributes(attr)

      if (seenOption.contains(woption.name)) {
        throw XProcException.xsDupWithOptionName(woption.name, location)
      }
      seenOption += woption.name

      addChild(woption)
    }

    // Now make sure there are no extra inputs
    val seenInput = mutable.HashSet.empty[String]
    for (winput <- children[WithInput]) {
      if (decl.inputPorts.isEmpty && winput.port == "") {
        throw XProcException.xsNoPrimaryInputPort(stepType, location)
      }
      if (!decl.inputPorts.contains(winput.port)) {
        throw XProcException.xsBadPortName(stepType, winput.port, location)
      }
      if (seenInput.contains(winput.port)) {
        throw XProcException.xsDupWithInputPort(winput.port, location)
      }
      seenInput += winput.port
    }

    // Now that we've checked the options, we can add an extra one.
    // If @p:message is given, treat it like a with-option (even though
    // it cannot be specified in that form).
    var msgName = if (stepType.getNamespaceURI == XProcConstants.ns_p) {
      XProcConstants._message
    } else {
      XProcConstants.p_message
    }

    if (attributes.contains(msgName)) {
      val woption = new WithOption(config, msgName)
      woption.staticContext = staticContext
      woption.avt = attr(msgName).get
      addChild(woption)
    }

    if (attributes.nonEmpty) {
      // On an atomic step, any left over attributes are presumably attempts
      // to use shortcuts for options that don't exist.
      throw XProcException.xsUndeclaredOption(stepType, attributes.keySet.head, location)
    }
  }

  override protected[model] def validateStructure(): Unit = {
    val iport = mutable.HashSet.empty[String]

    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case art: WithInput =>
          if (iport.contains(art.port)) {
            throw XProcException.xsDupWithInputPort(art.port, location)
          }
          iport += art.port
        case _: WithOutput => ()
        case _: WithOption => ()
        case _ =>
          throw new RuntimeException(s"Invalid content in atomic $this")
      }
    }
  }

  def stepImplementation(staticContext: XMLContext): StepExecutable = {
    stepImplementation(staticContext, None)
  }

  def stepImplementation(staticContext: XMLContext, implParams: Option[ImplParams]): StepExecutable = {
    val location = staticContext.location

    val sig = declaration(stepType).get
    val implClass = sig.implementation
    if (implClass.isEmpty) {
      val declStep = sig.declaration
      if (declStep.isDefined) {
        new StepRunner(config, declStep.get, sig)
      } else {
        throw new ModelException(ExceptionCode.NOIMPL, stepType.toString, location)
      }
    } else {
      val klass = Class.forName(implClass.head).getDeclaredConstructor().newInstance()
      klass match {
        case step: XmlStep =>
          new StepWrapper(step, sig)
        case _ =>
          throw new ModelException(ExceptionCode.IMPLNOTSTEP, stepType.toString, location)
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    _stepImplementation = stepImplementation(staticContext)
    _stepImplementation.configure(config, stepType, _name, params)

    _stepImplementation match {
      // If we're running an instance of a pipeline, work out which
      // ports actually received inputs (even p:empty) so that
      // we can tell the runtime for the pipeline. We need to do this
      // so that p:empty isn't misinterpreted as an unbound port.
      // Unbound ports will get default inputs if they're provided.
      case runner: StepRunner =>
        val usedPorts = mutable.HashSet.empty[String]
        for (input <- children[WithInput]) {
          val bound = input.children[DataSource].nonEmpty
          if (bound) {
            usedPorts += input.port
          }
        }
        runner.usedPorts(usedPorts.toSet)
      case _ => ()
    }

    var proxyParams: Option[ImplParams] = params

    // Handle any with-option values for this step that have been computed statically
    val staticallyComputed = mutable.HashMap.empty[String, Message]
    for (woption <- children[WithOption]) {
      if (woption.staticValue.isDefined) {
        staticallyComputed.put(woption.name.getClarkName, woption.staticValue.get)
      }
    }
    if (staticallyComputed.nonEmpty) {
      if (proxyParams.isDefined) {
        throw new RuntimeException("static options and constructed params?")
      }
      proxyParams = Some(new StepParams(staticallyComputed.toMap))
    }

    val pcontext = staticContext.withStatics(inScopeStatics)
    val proxy = new StepProxy(runtime, stepType, _stepImplementation, proxyParams, pcontext)
    val node = start.addAtomic(proxy, s"$stepType $stepName")
    _graphNode = Some(node)

    for (child <- children[WithOption]) {
      child.graphNodes(runtime, _graphNode.get)
    }

    // If there are any with-options that have name-bindings to preceding
    // options, patch those bindings to refer to the preceding with-options
    // See test ab-option-050
    val declOptions = mutable.HashMap.empty[QName, Node]
    for (child <- children[WithOption]) {
      if (!child.static) {
        if (child._graphNode.isDefined) {
          declOptions.put(child.name, child._graphNode.get)
        }
      }
    }
    for (nb <- findDescendants[NamePipe]) {
      if (declOptions.contains(nb.link.name)) {
        nb.patchNode(declOptions(nb.link.name))
      }
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)
    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startAtomic(tumble_id, stepName, stepType)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endAtomic()
  }

  override def toString: String = {
    s"$stepType $stepName"
  }
}
