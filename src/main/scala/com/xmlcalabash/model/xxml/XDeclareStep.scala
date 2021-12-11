package com.xmlcalabash.model.xxml

import com.jafpl.exceptions.JafplLoopDetected
import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XDeclareStep(config: XMLCalabash) extends XDeclContainer(config) {
  private var _visibility = Option.empty[String]
  private var _implclass = Option.empty[String]

  private var _computedApi = false
  private val _xinputs = mutable.HashMap.empty[String, XInput]
  private val _xoutputs = mutable.HashMap.empty[String, XOutput]

  def visibility: String = _visibility.getOrElse("public")
  def atomic: Boolean = children[XStep].isEmpty
  def stepType: Option[QName] = _type
  def implementationClass: Option[String] = _implclass

  // =======================================================================================

  def runtime(): XMLCalabashRuntime = {
    val runtime = new XMLCalabashRuntime(this)
    val pipeline = runtime.graph.addPipeline(stepName, manifold)

    for (port <- inputPorts) {
      runtime.graph.addInput(pipeline, port)
    }

    for (port <- outputPorts) {
      runtime.graph.addOutput(pipeline, port)
    }

    runtime.addNode(this, pipeline)

    graphNodes(runtime, pipeline)
    graphEdges(runtime)

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
    for (input <- children[XInput]) {
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
    for (child <- children[XOption]) {
      child.runtimeBindings(bindings)
    }
  }

  // =======================================================================================

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      _type = staticContext.parseQName(attr(XProcConstants._type))
      if (_type.isDefined) {
        if (_type.get.getNamespaceURI == "") {
          throw XProcException.xsBadStepTypeNamespace(location)
        }
        if (!config.standardLibraryParser && _type.get.getNamespaceURI == XProcConstants.ns_p) {
          throw XProcException.xsBadStepTypeNamespace(_type.get.getNamespaceURI, location)
        }
      }

      _psvi_required = staticContext.parseBoolean(attr(XProcConstants._psvi_required))

      if (attributes.contains(XProcConstants._xpath_version)) {
        val vstr = attr(XProcConstants._xpath_version).get
        _xpath_version = Some(vstr.toDouble)
      }

      if (attributes.contains(XProcConstants._version)) {
        val vstr = attr(XProcConstants._version).get
        try {
          _version = Some(vstr.toDouble)
          if (_version.get != 3.0) {
            error(XProcException.xsInvalidVersion(_version.get, None))
          }
        } catch {
          case _: NumberFormatException =>
            error(XProcException.xsBadVersion(vstr, None))
        }
      }
      if (_version.isEmpty && (parent.isEmpty || parent.get.synthetic)) {
        error(XProcException.xsVersionRequired(None))
      }

      _visibility = attr(XProcConstants._visibility)
      if (_visibility.isDefined) {
        if (_visibility.get != "public" && _visibility.get != "private") {
          error(XProcException.xdBadVisibility(_visibility.get, None))
        }
        if (parent.isDefined && parent.get.synthetic) {
          // If our parent is synthetic, then this was a p:declare-step imported directly
          // (and the library container has just been synthesized for consistency).
          // Directly imported p:declare-steps are always visible.
          _visibility = Some("public")
        }
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  // =======================================================================================
  // =======================================================================================
  // =======================================================================================
  // =======================================================================================

  /**
    * Sort out the inputs, outputs, and options of the step, ignoring its body.
    * This way, when we do look at the body of each step, we'll already have sorted
    * out the API of any steps it refers to.
    */
  protected[xxml] def elaborateStepApi(): Unit = {
    if (_computedApi) {
      return
    }
    _computedApi = true

    checkAttributes()
    checkEmptyAttributes()

    if (errors.nonEmpty) return

    xcomputeSignature()
  }

  private def xcomputeSignature(): Unit = {
    var primaryInput = Option.empty[XInput]
    var primaryOutput = Option.empty[XOutput]
    val staticOptions = ListBuffer.empty[XNameBinding]

    staticOptions ++= _precedingStaticOptions.toList
    staticContext = staticContext.withConstants(staticOptions.toList)

    // N.B. You can't re-order the children
    for (child <- allChildren) {
      child match {
        case input: XInput =>
          input.checkAttributes()
          input.checkEmptyAttributes()

          input.staticContext = input.staticContext.withConstants(staticOptions.toList)

          input.checkDefaultInputs()

          if (_xinputs.contains(input.port)) {
            input.error(XProcException.xsDupPortName(input.port, None))
          } else {
            _xinputs.put(input.port, input)
            if (input.primary) {
              if (primaryInput.isDefined) {
                input.error(XProcException.xsDupPrimaryInputPort(input.port, primaryInput.get.port, None))
              } else {
                primaryInput = Some(input)
              }
            }
          }
        case output: XOutput =>
          output.checkAttributes()
          output.checkEmptyAttributes()

          output.staticContext = output.staticContext.withConstants(staticOptions.toList)

          if (_xoutputs.contains(output.port)) {
            output.error(XProcException.xsDupPortName(output.port, None))
          } else {
            _xoutputs.put(output.port, output)
            if (output.primary) {
              if (primaryOutput.isDefined) {
                output.error(XProcException.xsDupPrimaryOutputPort(output.port, primaryOutput.get.port, None))
              } else {
                primaryOutput = Some(output)
              }
            }
          }
        case option: XOption =>
          option.checkAttributes()
          option.checkEmptyAttributes()
          if (_xoptions.contains(option.name)) {
            error(XProcException.xsDuplicateOptionName(option.name, None))
          } else {
            _xoptions.put(option.name, option)
          }
          if (option.static) {
            staticOptions += option
          }

        case _ =>
          ()
      }
    }

    if (primaryInput.isEmpty && inputPorts.size == 1) {
      val input = children[XInput].head
      if (!input.primarySpecified) {
        input.primary = true
      }
    }

    if (primaryOutput.isEmpty && outputPorts.size == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }
  }

  override protected[xxml] def validate(): Unit = {
    val seenPorts = mutable.HashSet.empty[String]
    val newChildren = ListBuffer.empty[XArtifact]

    // N.B. You can't re-order the children
    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        // N.B. input, output, and option were validated in elaborateStepApi()
        case input: XInput =>
          if (seenPorts.contains(input.port)) {
            input.error(XProcException.xsDupPortName(input.port, None))
          } else {
            seenPorts += input.port
          }
          newChildren += input
        case output: XOutput =>
          if (seenPorts.contains(output.port)) {
            output.error(XProcException.xsDupPortName(output.port, None))
          } else {
            seenPorts += output.port
          }
          newChildren += output
        case option: XOption =>
          newChildren += option
        case _: XImport =>
          ()
        case imp: XImportFunctions =>
          newChildren += imp
        case v: XVariable =>
          newChildren += v
        case step: XStep =>
          newChildren += step
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = newChildren.toList

    checkStepNameScoping(Set())

    if (exceptions.nonEmpty) {
      return
    }

    if (atomic) {
      if (stepType.isDefined) {
        _implclass = config.externalSteps.get(stepType.get)
      }
      return
    }

    if (children[XOutput].length == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }

    constructDefaultOutput()

    if (exceptions.isEmpty) {
      elaborateSyntacticSugar()
    }

    if (exceptions.isEmpty) {
      elaborateDefaultReadablePort(None)
    }

    if (exceptions.isEmpty) {
      var context = new XNameBindingContext()
      for (static <- _precedingStaticOptions) {
        context = context.withBinding(static)
      }
      elaborateNameBindings(context)
    }

    if (exceptions.isEmpty) {
      hoistSourcesToPipes()
    }

    if (exceptions.isEmpty) {
      elaboratePortConnections()
      if (exceptions.isEmpty) {
        hoistSourcesToPipes()
      }
    }

    if (exceptions.isEmpty) {
      elaborateValidatePortConnections(new XPortBindingContext())
      if (exceptions.isEmpty) {
        hoistSourcesToPipes()
        if (exceptions.isEmpty) {
          elaborateValidatePortConnections(new XPortBindingContext())
        }
      }
    }

    if (exceptions.isEmpty) {
      elaborateDependsConnections(Map())
    }

    if (exceptions.isEmpty) {
      elaborateDynamicVariables()
    }

    if (exceptions.isEmpty) {
      elaborateDynamicOptions()
    }

    if (exceptions.isEmpty) {
      elaborateInsertSelectFilters()
      if (exceptions.isEmpty) {
        elaborateValidatePortConnections(new XPortBindingContext())
      }
    }

    if (exceptions.isEmpty) {
      elaborateInsertContentTypeFilters()
      if (exceptions.isEmpty) {
        elaborateValidatePortConnections(new XPortBindingContext())
      }
    }

    if (exceptions.isEmpty) {
      computeReadsFrom()
    }
  }

  // =======================================================================================
  // =======================================================================================
  // =======================================================================================
  // =======================================================================================

  override protected[xxml] def elaborateInsertContentTypeFilters(): Unit = {
    if (!atomic) {
      super.elaborateInsertContentTypeFilters()
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    if (_type.isDefined) {
      attr.put("type", Some(_type.get.getEQName))
    }
    attr.put("name", Some(stepName))
    dumpTree(sb, "p:declare-step", attr.toMap)
  }

  override def toString: String = {
    if (_type.isDefined) {
      s"${_type.get}: ${stepName}"
    } else {
      stepName
    }
  }
}
