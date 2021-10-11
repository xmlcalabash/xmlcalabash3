package com.xmlcalabash.model.xml

import com.jafpl.exceptions.JafplLoopDetected
import com.jafpl.graph.Node
import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.config.{OptionSignature, PortSignature, StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.runtime.params.ContentTypeCheckerParams
import com.xmlcalabash.util.xc.ElaboratedPipeline
import com.xmlcalabash.util.{S9Api, TypeUtils, XProcVarValue}
import net.sf.saxon.s9api.{ItemType, QName, SaxonApiException, XdmAtomicValue, XdmNode, XdmNodeKind}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeclareStep(override val config: XMLCalabashConfig) extends DeclContainer(config) {
  private var _type = Option.empty[QName]
  private var _visibility = Option.empty[String]
  private var _signature = Option.empty[StepSignature]
  private val _inputs = mutable.HashMap.empty[String, DeclareInput]
  private val _bindings = mutable.HashMap.empty[QName, DeclareOption]
  private var _excludeUriBindings = Set.empty[String]

  // These are a bit of a hack. It's very hard to tell the difference
  // between an inline declare-step and an imported declare-step.
  // We have to process them, but we have to not process them twice.
  private var madeStructureExplicit = false
  private var madeBindingsExplicit = false
  private var didValidateStructure = false

  def stepType: Option[QName] = _type
  def psvi_required: Option[Boolean] = _psvi_required
  def xpath_version: Option[Double] = _xpath_version
  def exclude_inline_prefixes: Option[String] = _exclude_inline_prefixes
  def exclude_uri_bindings: Set[String] = _excludeUriBindings
  def version: Double = _version.getOrElse(3.0)
  def visiblity: String = _visibility.getOrElse("public")
  def signature: Option[StepSignature] = _signature

  def inputPorts: List[String] = _inputs.keySet.toList
  def outputPorts: List[String] = _outputs.keySet.toList

  def input(port: String): DeclareInput = _inputs(port)
  def output(port: String): DeclareOutput = _outputs(port)

  def inputs: List[DeclareInput] = _inputs.values.toList
  def outputs: List[DeclareOutput] = _outputs.values.toList

  def bindings: Map[QName,DeclareOption] = _bindings.toMap

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    val aname = attr(XProcConstants._name)
    if (aname.isDefined) {
      try {
        val typeUtils = new TypeUtils(config)
        val ncname = typeUtils.castAtomicAs(XdmAtomicValue.makeAtomicValue(aname.get), ItemType.NCNAME, staticContext)
        _name = Some(ncname.getStringValue)
      } catch {
        case _: SaxonApiException =>
          throw XProcException.xsBadTypeValue(aname.get, "NCName", location)
      }
    }

    _type = staticContext.parseQName(attr(XProcConstants._type))
    _psvi_required = staticContext.parseBoolean(attr(XProcConstants._psvi_required))
    if (attributes.contains(XProcConstants._xpath_version)) {
      val vstr = attr(XProcConstants._xpath_version).get
      _xpath_version = Some(vstr.toDouble)
    }
    if (attributes.contains(XProcConstants._version)) {
      val vstr = attr(XProcConstants._version).get
      try {
        _version = Some(vstr.toDouble)
      } catch {
        case _: NumberFormatException =>
          throw XProcException.xsBadVersion(vstr, location)
      }
      if (_version.get != 3.0) {
        throw XProcException.xsInvalidVersion(_version.get, location)
      }
    }
    if (_version.isEmpty) {
      if (node.getParent.getNodeKind == XdmNodeKind.DOCUMENT) {
        throw XProcException.xsVersionRequired(location)
      }
    }

    _exclude_inline_prefixes = attr(XProcConstants._exclude_inline_prefixes)
    if (_exclude_inline_prefixes.isDefined) {
      val prefixes = _exclude_inline_prefixes.get.split("\\s+").toList
      _excludeUriBindings = S9Api.urisForPrefixes(node, prefixes.toSet)
    }

    _visibility = attr(XProcConstants._visibility)
    if (_visibility.isDefined) {
      if (_visibility.get != "public" && _visibility.get != "private") {
        throw XProcException.xdBadVisibility(_visibility.get, location)
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  protected[xml] def parseDeclarationSignature(): Unit = {
    // If there's only one input and it doesn't have a declared primary status, make it primary
    val pinput = children[DeclareInput].headOption
    if (children[DeclareInput].length == 1 && pinput.get._primary.isEmpty) {
      pinput.get.primary = true
    }

    // If any of the inputs have defaults, record them
    for (declinput <- children[DeclareInput]) {
      declinput.examineBindings()
      if (declinput.children[DataSource].nonEmpty) {
        declinput.defaultInputs ++= declinput.children[DataSource]
        // then remove them
        declinput.removeChildren()
      }
    }

    // If there's only one output and it doesn't have a declared primary status, make it primary
    val poutput = children[DeclareOutput].headOption
    if (children[DeclareOutput].length == 1 && poutput.get._primary.isEmpty) {
      poutput.get.primary = true
    }

    var lastInput = Option.empty[DeclareInput]
    var lastOutput = Option.empty[DeclareOutput]
    var primaryInput = Option.empty[DeclareInput]
    var primaryOutput = Option.empty[DeclareOutput]

    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          if (_inputs.contains(input.port) || _outputs.contains(input.port)) {
            throw XProcException.xsDupPortName(input.port, location)
          }
          _inputs.put(input.port, input)

          lastInput = Some(input)
          if (input.primary) {
            if (primaryInput.isDefined) {
              throw XProcException.xsDupPrimaryPort(input.port, primaryInput.get.port, staticContext.location)
            }
            primaryInput = Some(input)
          }
        case output: DeclareOutput =>
          if (_outputs.contains(output.port) || _inputs.contains(output.port)) {
            throw XProcException.xsDupPortName(output.port, location)
          }
          _outputs.put(output.port, output)

          lastOutput = Some(output)
          if (output.primary) {
            if (primaryOutput.isDefined) {
              throw XProcException.xsDupPrimaryPort(output.port, primaryOutput.get.port, staticContext.location)
            }
            primaryOutput = Some(output)
          }
        case option: DeclareOption =>
          if (_bindings.contains(option.name)) {
            throw new RuntimeException("duplicate option name")
          }
          _bindings.put(option.name, option)

        case _ => ()
      }
    }

    if (_inputs.size == 1 && lastInput.get._primary.isEmpty) {
      lastInput.get.primary = true
    }

    if (_outputs.size == 1 && lastOutput.get._primary.isEmpty) {
      lastOutput.get.primary = true
    }

    val stepSig = new StepSignature(stepType)
    if (stepType.isDefined) {
      if (config.atomicStepImplementation(stepType.get).isDefined) {
        stepSig.implementation = config.atomicStepImplementation(stepType.get).get
      } else {
        stepSig.declaration = this
        if (atomic) {
          logger.info(s"No implementation for declared ${stepType.get} step")
        }
      }
    }

    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          val portSig = new PortSignature(input.port, input.primary, input.sequence, input.defaultInputs.toList)
          portSig.contentTypes = input.contentTypes
          stepSig.addInput(portSig, input.location.get)
        case output: DeclareOutput =>
          val portSig = new PortSignature(output.port, output.primary, output.sequence)
          portSig.contentTypes = output.contentTypes
          stepSig.addOutput(portSig, output.location.get)
        case option: DeclareOption =>
          val optSig = new OptionSignature(option.name, option.declaredType, option.required)
          if (option.allowedValues.isDefined) {
            optSig.tokenList = option.allowedValues.get
          }
          if (option.select.isDefined) {
            optSig.defaultSelect = option.select.get
          }
          optSig.forceQNameKeys = option.qnameKeys
          stepSig.addOption(optSig, option.location.get)
        case _ =>
          ()
      }
    }

    _signature = Some(stepSig)
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    if (madeStructureExplicit) {
      return
    }
    madeStructureExplicit = true

    super.makeStructureExplicit()

    // If any of the inputs have defaults, make sure we make the structure explicit
    for (declinput <- children[DeclareInput]) {
      for (source <- declinput.defaultInputs) {
        source.makeStructureExplicit()
      }
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    if (madeBindingsExplicit) {
      return
    }
    madeBindingsExplicit = true

    super.makeBindingsExplicit()

    // If any of the inputs have defaults, make sure we make the bindings explicit
    for (declinput <- children[DeclareInput]) {
      for (source <- declinput.defaultInputs) {
        source.makeBindingsExplicit()
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    if (didValidateStructure) {
      return
    }
    didValidateStructure = true

    val buf = ListBuffer.empty[Artifact]

    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case decl: DeclareStep =>
          buf += decl
        case _ => ()
      }
    }

    for (decl <- buf) {
      removeChild(decl)
    }

    if (!atomic) {
      normalizeToPipes()
      addFilters()
    }
  }

  def runtime(): XMLCalabashRuntime = {
    val runtime = new XMLCalabashRuntime(this)
    val pipeline = runtime.graph.addPipeline(stepName, manifold)

    for (port <- inputPorts) {
      runtime.graph.addInput(pipeline, port)
    }

    for (port <- outputPorts) {
      runtime.graph.addOutput(pipeline, port)
    }

    _graphNode = Some(pipeline)

    graphNodes(runtime, pipeline)

    // Look for name bindings that have to be patched
    val declOptions = mutable.HashMap.empty[NameBinding, Node]
    for (child <- children[DeclareOption]) {
      if (!child.static) {
        declOptions.put(child, child._graphNode.get)
      }
    }
    for (nb <- findDescendants[NamePipe]) {
      if (declOptions.contains(nb.link)) {
        nb.patchNode(declOptions(nb.link))
      }
    }

    for (child <- allChildren) {
      child match {
        case _: Documentation => ()
        case _: PipeInfo => ()
        case _ =>
          child.graphEdges(runtime, pipeline)
      }
    }

    try {
      runtime.init(this)
    } catch {
      case _: JafplLoopDetected =>
        throw XProcException.xsLoop("???", "???", location)
    }
    runtime
  }

  private def manifold: Manifold = {
    val inputMap = mutable.HashMap.empty[String,PortCardinality]
    for (input <- inputs) {
      if (input.sequence) {
        inputMap.put(input.port, PortCardinality.ZERO_OR_MORE)
      } else {
        if (input.defaultInputs.nonEmpty) {
          // Allow it to be empty, the default will provide something
          inputMap.put(input.port, new PortCardinality(0, 1))
        } else {
          inputMap.put(input.port, PortCardinality.EXACTLY_ONE)
        }
      }
    }
    val outputMap = mutable.HashMap.empty[String,PortCardinality]
    for (output <- outputs) {
      if (output.sequence) {
        outputMap.put(output.port, PortCardinality.ZERO_OR_MORE)
      } else {
        outputMap.put(output.port, PortCardinality.EXACTLY_ONE)
      }
    }
    new Manifold(new PortSpecification(inputMap.toMap), new PortSpecification(outputMap.toMap))
  }

  def patchOptions(bindings: Map[QName,XProcVarValue]): Unit = {
    for (child <- children[DeclareOption]) {
      child.runtimeBindings(bindings)
    }
  }

  def xdump: XdmNode = {
    val xml = new ElaboratedPipeline(config)
    xdump(xml)
    val doc = xml.endPipeline()
    doc.get
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startPipeline(tumble_id, stepName, stepType, version, psvi_required, xpath_version, exclude_inline_prefixes, _visibility)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
  }

  override def toString: String = {
    if (stepType.isDefined) {
      s"p:declare-step ${stepType.get} $stepName $uid"
    } else {
      s"p:declare-step {anon} $stepName $uid"
    }
  }

}
