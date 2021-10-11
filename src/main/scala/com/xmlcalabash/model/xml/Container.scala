package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XmlPortSpecification}
import com.xmlcalabash.runtime.params.{ContentTypeCheckerParams, SelectFilterParams}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Container(override val config: XMLCalabashConfig) extends Step(config) with NamedArtifact {
  protected var _outputs = mutable.HashMap.empty[String, DeclareOutput]

  def atomic: Boolean = {
    for (child <- allChildren) {
      child match {
        case _: AtomicStep =>
          // Synthetic children (inline-loader, etc.) don't count
          if (!child.synthetic) {
            return false
          }
        case _: Container => return false
        case _ => ()
      }
    }
    true
  }

  protected[model] def makeContainerStructureExplicit(): Unit = {
    var firstChild = Option.empty[Artifact]
    var withInput = Option.empty[WithInput]
    var lastOutput = Option.empty[DeclareOutput]
    var primaryOutput = Option.empty[DeclareOutput]
    var lastStep = Option.empty[Step]

    for (child <- allChildren) {
      child.makeStructureExplicit()

      if (firstChild.isEmpty) {
        firstChild = Some(child)
      }

      child match {
        case input: WithInput =>
          if (withInput.isDefined) {
            throw new RuntimeException("Only one with-input is allowed")
          }
          withInput = Some(input)
        case output: DeclareOutput =>
          if (_outputs.contains(output.port)) {
            throw new RuntimeException("duplicate output port")
          }
          _outputs.put(output.port, output)

          lastOutput = Some(output)
          if (output.primary) {
            if (primaryOutput.isDefined) {
              throw XProcException.xsDupPrimaryPort(output.port, primaryOutput.get.port, staticContext.location)
            }
            primaryOutput = Some(output)
          }
        case atomic: AtomicStep =>
          lastStep = Some(atomic)
        case compound: Container =>
          lastStep = Some(compound)
        case variable: Variable =>
          environment().addVariable(variable)
        case _ =>
          ()
      }
    }

    if (_outputs.isEmpty && lastStep.isDefined && lastStep.get.primaryOutput.isDefined) {
      val output = syntheticDeclaredOutput()

      val pipe = new Pipe(config)
      pipe.step = lastStep.get.stepName
      pipe.port = lastStep.get.primaryOutput.get.port
      pipe.link = lastStep.get.primaryOutput.get
      output.addChild(pipe)
      _outputs.put(output.port, output)
      lastOutput = Some(output)

      if (firstChild.isDefined) {
        addChild(output, firstChild.get)
      } else {
        addChild(output)
      }
    }

    if (_outputs.size == 1 && lastOutput.get._primary.isEmpty) {
      lastOutput.get.primary = true
    }
  }

  protected def syntheticDeclaredOutput(): DeclareOutput = {
    val output = new DeclareOutput(config)
    output.port = "#result"
    output.primary = true
    output.sequence = true
    output
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    val env = environment()

    // Be careful here, we don't call super.makeBindingsExplicit() so this method
    // has to be kept up-to-date with respect to changes there!

    // Make the bindings for this step's inputs explicit
    for (input <- children[WithInput]) {
      input.makeBindingsExplicit()
    }

    for (sbinding <- env.staticVariables) {
      _inScopeStatics.put(sbinding.name.getClarkName, sbinding)
    }

    for (dbinding <- env.variables) {
      _inScopeDynamics.put(dbinding.name, dbinding)
    }

    if (atomic) {
      return
    }

    var poutput = Option.empty[DeclareOutput]
    var lastStep = Option.empty[Step]
    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          if (output.primary) {
            poutput = Some(output)
          }
        case step: Step =>
          lastStep = Some(step)
        case _ => ()
      }
    }

    if (lastStep.isDefined && poutput.isDefined && poutput.get.bindings.isEmpty) {
      val lpo = lastStep.get.primaryOutput
      if (lpo.isDefined) {
        val pipe = new Pipe(config)
        pipe.port = lpo.get.port
        pipe.step = lastStep.get.stepName
        pipe.link = lpo.get
        poutput.get.addChild(pipe)
      }
    }

    for (child <- allChildren) {
      child match {
        case option: DeclareOption =>
          option.makeBindingsExplicit()
          if (option.select.isDefined && option.static) {
            option.staticValue = computeStatically(option.select.get)
          }
        case _: WithInput =>
          () // we did this above
        case _ =>
          child.makeBindingsExplicit()
      }
    }

    // Add ContentTypeCheckers and select filters if we need them
    val innerEnv = lastStep.get.environment()
    for (input <- children[DeclareInput]) {
      if (input.select.isDefined) {
        logger.debug(s"Adding select filter for container input ${stepName}/${input.port}: ${input.select.get}")
        val context = staticContext.withStatics(inScopeStatics)

        val ispec = if (input.sequence) {
          XmlPortSpecification.ANYSOURCESEQ
        } else {
          XmlPortSpecification.ANYSOURCE
        }

        val params = new SelectFilterParams(context, input.select.get, input.port, ispec)
        val filter = new AtomicStep(config, params)
        filter.stepType = XProcConstants.cx_select_filter

        addChild(filter)

        val finput = new WithInput(config)
        finput.port = "source"
        finput.primary = true
        filter.addChild(finput)

        val foutput = new WithOutput(config)
        foutput.port = "result"
        foutput.primary = true
        filter.addChild(foutput)

        for (name <- staticContext.findVariableRefsInString(input.select.get)) {
          var binding = _inScopeDynamics.get(name)
          if (binding.isDefined) {
            val npipe = new NamePipe(config, name, binding.get.tumble_id, binding.get)
            filter.addChild(npipe)
          } else {
            binding = _inScopeStatics.get(name.getClarkName)
            if (binding.isEmpty) {
              throw new RuntimeException(s"Reference to variable not in scope: $name")
            }
          }
        }

        replumb(input, foutput)

        val newpipe = new Pipe(config)
        newpipe.step = stepName
        newpipe.port = input.port
        newpipe.link = input
        finput.addChild(newpipe)
      }

      if (!MediaType.OCTET_STREAM.allowed(input.contentTypes)) {
        logger.debug(s"Adding content-type-checker for container input ${stepName}/${input.port}")
        val params = new ContentTypeCheckerParams(input.port, input.contentTypes, staticContext, None, inputPort = true, true)
        val atomic = new AtomicStep(config, params)
        atomic.stepType = XProcConstants.cx_content_type_checker
        addChild(atomic)

        val winput = new WithInput(config)
        winput.port = "source"
        atomic.addChild(winput)

        val woutput = new WithOutput(config)
        woutput.port = "result"
        atomic.addChild(woutput)

        replumb(input, woutput)

        val newpipe = new Pipe(config)
        newpipe.step = stepName
        newpipe.port = input.port
        newpipe.link = input
        winput.addChild(newpipe)
      }
    }

    for (output <- children[DeclareOutput]) {
      var check = false
      //println(s"${stepName}, ${output.port}, ${output.contentTypes}")
      for (binding <- output.children[Pipe]) {
        val port = binding.port
        val step = innerEnv.step(binding.step).get
        val outputContentTypes = step match {
          case atom: AtomicStep =>
            val decl = atom.declaration(atom.stepType).get
            decl.output(port, None).contentTypes
          case cont: Container =>
            if (cont._outputs.contains(port)) {
              cont._outputs(port).contentTypes
            } else {
              // See if we can find a with-output for this port
              var woutput = Option.empty[WithOutput]
              for (wo <- cont.children[WithOutput]) {
                if (wo.port == port) {
                  woutput = Some(wo)
                }
              }
              if (woutput.isEmpty) {
                // It must be implicit, so no checking is required
                List(MediaType.OCTET_STREAM)
              } else {
                woutput.get.contentTypes
              }
            }
        }

        //println(s"   ${step} / ${outputContentTypes}")
        for (pct <- outputContentTypes) {
          check = check || !pct.allowed(output.contentTypes)
        }
      }

      if (check) {
        logger.debug(s"Adding content-type-checker for container output ${stepName}/${output.port}")
        val params = new ContentTypeCheckerParams(output.port, output.contentTypes, staticContext, None, inputPort = false, true)
        val atomic = new AtomicStep(config, params)
        atomic.stepType = XProcConstants.cx_content_type_checker
        addChild(atomic)

        val winput = new WithInput(config)
        winput.port = "source"
        atomic.addChild(winput)

        val pipes = ListBuffer.empty[Pipe] ++ output.children[Pipe]
        for (oldpipe <- pipes) {
          output.removeChild(oldpipe)
          winput.addChild(oldpipe)
        }

        val woutput = new WithOutput(config)
        woutput.port = "result"
        atomic.addChild(woutput)

        val newpipe = new Pipe(config)
        newpipe.step = atomic.stepName
        newpipe.port = "result"
        newpipe.link = woutput
        output.addChild(newpipe)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case _: WithInput => ()
        case _: WithOutput => ()
        case _: DeclareInput => ()
        case _: DeclareOutput => ()
        case _: Step => ()
        case _: NamePipe => () // For Viweport
        case _: Variable => ()
        case _ =>
          throw new RuntimeException(s"Unexpected content in $this: $child")
      }
    }
  }

  def containerManifold: Manifold = {
    val spec = mutable.HashMap.empty[String, PortCardinality]
    for (output <- _outputs.values) {
      if (output.sequence) {
        spec.put(output.port, PortCardinality.ZERO_OR_MORE)
      } else {
        spec.put(output.port, PortCardinality.EXACTLY_ONE)
      }
    }
    new Manifold(Manifold.WILD, new PortSpecification(spec.toMap))
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (allChildren.nonEmpty) {
      if (_graphNode.isDefined) {
        for (child <- allChildren) {
          child match {
            case step: Step =>
              step.graphNodes(runtime, _graphNode.get)
            case option: DeclareOption =>
              option.graphNodes(runtime, _graphNode.get)
            case variable: Variable =>
              variable.graphNodes(runtime, _graphNode.get)
            case _ => ()
          }
        }
      } else {
        println("cannot graphNodes for children of " + this)
      }
    }
  }
}
