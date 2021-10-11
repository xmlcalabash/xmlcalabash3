package com.xmlcalabash.model.xml

import com.sun.org.apache.xpath.internal.XPathProcessorException
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// You'd think that the environment just inherited down the tree as you went,
// but it's more complicated than that. What's in and out of the environment
// is quite different inside a compound step than it is outside. So instead of
// trying to finesse it as we walk down the tree, this object computes the
// environment for the step identified in its constructor.
//
// N.B. Do not cache this; you'd think it would be a performance enhancement,
// but dealing with select expressions sometimes adds filters and that
// changes the default readable port.

object Environment {
  def newEnvironment(step: Artifact): Environment = {
    // Walk "up" the tree until we find a valid starting point
    step match {
      case _: Step => ()
      case _: DeclareInput => ()
      case _: DeclareOutput => ()
      case _: Variable => ()
      case _: DeclareOption => ()
      case _: WithOption => ()
      case _ =>
        return Environment.newEnvironment(step.parent.get)
    }

    val env = new Environment()

    var ancestors = ListBuffer.empty[Artifact]    // includes self for compound steps

    // If we start at a DeclareOption, then we're really in a pipeline that we're
    // running for a step. Stop at the first atomic step ancestor...
    val stopAtStep = step.isInstanceOf[DeclareOption]
    var scopeRoot = Option.empty[Artifact]

    var pptr: Option[Artifact] = Some(step)
    while (pptr.isDefined) {
      var next = pptr.get.parent
      pptr.get match {
        case variable: Variable =>
          ancestors.insert(0, variable)
        case option: DeclareOption =>
          ancestors.insert(0, option)
        case input: DeclareInput =>
          ancestors.insert(0, input)
        case output: DeclareOutput =>
          ancestors.insert(0, output)
        case library: Library =>
          ancestors.insert(0, library)
        case option: WithOption =>
          ancestors.insert(0, option)
        case step: Step =>
          if (stopAtStep && scopeRoot.isEmpty) {
            scopeRoot = Some(step)
          }
          ancestors.insert(0, step)
        case _ => ()
      }

      pptr = next
    }

    if (scopeRoot.isDefined) {
      // The scopeRoot is really the top of the ancestor list, *except* that any static
      // variables declared "above" this root are in scope.
      val senv = Environment.newEnvironment(scopeRoot.get)
      for (opt <- senv.staticVariables) {
        if (opt ne step) {
          env.addVariable(opt)
        }
      }

      while (ancestors.nonEmpty && (ancestors.head ne scopeRoot.get)) {
        ancestors = ancestors.drop(1)
      }
      if (ancestors.isEmpty) {
        throw XProcException.xiThisCantHappen("Failed to find scopeRoot in ancestors when creating new environment", None)
      }
      ancestors = ancestors.drop(1)
    }

    walk(env, ancestors.toList)
  }

  private def walk(env: Environment, ancestors: List[Artifact]): Environment = {
    if (ancestors.isEmpty) {
      return env
    }

    val head = ancestors.head
    val next = ancestors.tail.headOption

    if (head.isInstanceOf[DeclareStep]) {
      // The only thing that survives passing into a declare step are statics
      env.clearSteps()
      env.clearPorts()
      env.clearDynamicVariables()
    }

    head match {
      case wi: WithOption =>
        if (wi.parent.isDefined) {
          wi.parent.get match {
            case atom: AtomicStep =>
              val stepsig = atom.declaration(atom.stepType)
              // This appears not to be defined for built-in steps. I'm not sure why, but none
              // of them have options that depend on preceding options, so I'm not worrying
              // about it today.
              if (stepsig.get.declaration.isDefined) {
                val decl = stepsig.get.declaration.get
                var found = false
                for (opt <- decl.children[DeclareOption]) {
                  found = found || opt.name == wi.name
                  if (!found) {
                    env.addVariable(opt)
                  }
                }
              }
              env
            case _ =>
              throw XProcException.xiThisCantHappen("Parent of p:with-option is not an atomic step?", None)
          }
        } else {
          throw XProcException.xiThisCantHappen("Parent of p:with-option is undefined?", None)
        }
      case lib: Library =>
        env.defaultReadablePort = None

        // Libraries are a special case, they aren't in the children of the container
        if (next.get.isInstanceOf[Library]) {
          return walk(env, ancestors.tail)
        }

        // Now walk down to the next ancestor
        for (child <- lib.allChildren) {
          if (next.get == child) {
            return walk(env, ancestors.tail)
          }

          child match {
            case variable: Variable =>
              env.addVariable(variable)
            case option: DeclareOption =>
              env.addVariable(option)
            case childstep: Step =>
              if (next.isDefined && next.get == childstep) {
                return walk(env, ancestors.tail)
              }
            case _ => ()
          }
        }

        // If we fell off the bottom of this loop, something has gone terribly wrong
        throw new RuntimeException("Fell off ancestor list in computing environment")

      case xstep: Container =>
        // DeclareStep is special; if it's the last ancestor, then it's the root of
        // the pipeline and that doesn't get to read its own inputs. If it's not
        // the root, then we're setting up the environment for one of its contained
        // steps.

        // This step is in the environment
        env.addStep(xstep)

        // Its options are in-scope
        for (option <- xstep.children[DeclareOption]) {
          env.addVariable(option)
        }

        if (next.isDefined) {
          // Its inputs are readable
          for (port <- xstep.children[DeclareInput]) {
            if (port.primary) {
              env.defaultReadablePort =  port
            }
            env.addPort(port)
          }

          xstep match {
            // Choose, when, etc., aren't ordinary container steps
            case container: Choose => ()
            //case container: When => ()
            //case container: Otherwise => ()
            case container: Try => ()
            //case container: Catch => ()
            //case container: Finally => ()
            case _ =>
              // Entering a declare-step resets the default readable port
              if (xstep.isInstanceOf[DeclareStep]) {
                env.defaultReadablePort = xstep.primaryInput
              }

              // The outputs of all contained steps are mutually readable
              for (child <- xstep.allChildren) {
                child match {
                  case decl: DeclareStep => () // these don't count
                  case childstep: Container =>
                    if (next.isDefined && next.get == childstep) {
                      // ignore this one, we'll be diving down into it
                    } else {
                      env.addStep(childstep)
                      for (port <- childstep.children[WithOutput]) {
                        env.addPort(port)
                      }
                    }
                  case childstep: Step =>
                    // Yes, this can add the output of the step who's environment
                    // we're computing to the list of readable ports. Doing so
                    // is a loop, it'll be caught elsewhere.
                    env.addStep(childstep)
                    for (port <- childstep.children[WithOutput]) {
                      env.addPort(port)
                    }
                  case _ => ()
                }
              }
          }
        }

        if (next.isEmpty) {
          return env
        }

        // Libraries are a special case, they aren't in the children of the container
        if (next.get.isInstanceOf[Library]) {
          return walk(env, ancestors.tail)
        }

        // Now walk down to the next ancestor, calculating the drp
        for (child <- xstep.allChildren) {
          if (next.get eq child) {
            return walk(env, ancestors.tail)
          }
          xstep match {
            // The children of choose and try aren't ordinary children
            case _: Choose => ()
            case _: Try => ()
            case _ =>
              child match {
                case option: DeclareOption =>
                  env.addVariable(option)
                case variable: Variable =>
                  env.addVariable(variable)
                case _: DeclareStep =>
                  ()
                case childstep: Step =>
                  env.defaultReadablePort = childstep.primaryOutput
                case _ =>
                  ()
              }
          }
        }

        next.get match {
          case _: DeclareStep =>
            return walk(env, ancestors.tail)
          case _ =>
            // If we fell off the bottom of this loop, something has gone terribly wrong
            throw new RuntimeException("Fell off ancestor list in container")
        }

      case step: AtomicStep =>
        if (next.isEmpty) {
          // This is us.
          return env
        } else {
          return walk(env, ancestors.tail)
        }

      case option: DeclareOption =>
        if (next.isEmpty) {
          // This is us. Any preceding options are in-scope
          val parent = option.parent.get
          var found = false
          for (child <- parent.allChildren) {
            child match {
              case opt: DeclareOption =>
                found = found || (opt eq option)
                if (!found) {
                  env.addVariable(opt)
                }
              case _ => ()
            }
          }
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Option with children?")

      case variable: Variable =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Variable with children?")

      case input: DeclareInput =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Input with children?")

      case output: DeclareOutput =>
        if (next.isEmpty) {
          // This is us.
          // The default readable port from here is the primary output
          // of the last step in the pipeline, if the last step has
          // a primary output.
          var lastchild = Option.empty[Step]
          for (child <- output.parent.get.children[Step]) {
            lastchild = Some(child)
          }
          if (lastchild.isDefined) {
            env.defaultReadablePort = lastchild.get.primaryOutput
          }

          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Output with children?")

      case _ => throw new RuntimeException(s"Unexpected in list of ancestors: $head")
    }
  }
}

class Environment private() {
  private val _inScopeSteps = mutable.HashMap.empty[String, Step]
  private val _inScopePorts = mutable.HashMap.empty[String, Port]
  private val _inScopeVariables = mutable.HashMap.empty[QName,NameBinding]
  private var _defaultReadablePort = Option.empty[Port]

  def defaultReadablePort: Option[Port] = _defaultReadablePort
  protected[xml] def defaultReadablePort_=(port: Port): Unit = {
    defaultReadablePort = Some(port)
  }
  protected[xml] def defaultReadablePort_=(port: Option[Port]): Unit = {
    _defaultReadablePort = port
  }
  private def clearSteps(): Unit = {
    _inScopeSteps.clear()
  }

  def addStep(step: Step): Unit = {
    addName(step.stepName, step)
  }

  private def clearPorts(): Unit = {
    _inScopePorts.clear()
  }

  def addPort(port: Port): Unit = {
    val name = port.parent.get match {
      case step: Step => step.stepName
      case _ =>
        throw new RuntimeException("Parent of port isn't a step?")
    }
    if (!name.startsWith("!")) {
      _inScopePorts.put(s"$name/${port.port}", port)
    }
  }

  private def addName(name: String, step: Step): Unit = {
    if (_inScopeSteps.contains(name)) {
      throw XProcException.xsDuplicateStepName(name, None)
    }
    _inScopeSteps.put(name, step)
  }

  def addVariable(binding: NameBinding): Unit = {
    _inScopeVariables.put(binding.name, binding)
  }

  private def clearVariables(): Unit = {
    _inScopeVariables.clear()
  }

  private def clearDynamicVariables(): Unit = {
    val statics = mutable.HashMap.empty[QName,NameBinding]
    for (static <- staticVariables) {
      statics.put(static.name, static)
    }
    _inScopeVariables.clear()
    _inScopeVariables ++= statics
  }


  def step(name: String): Option[Step] = _inScopeSteps.get(name)

  def port(stepName: String, portName: String): Option[Port] = {
    if (step(stepName).isDefined) {
      _inScopePorts.get(s"$stepName/$portName")
    } else {
      None
    }
  }

  def variable(name: QName): Option[NameBinding] = {
    if (_inScopeVariables.contains(name)) {
      _inScopeVariables.get(name)
    } else {
      None
    }
  }

  def variables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    for ((name, variable) <- _inScopeVariables) {
      if (!variable.static) {
        lb += variable
      }
    }
    lb.toList
  }

  def staticVariables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    for ((name, variable) <- _inScopeVariables) {
      if (variable.static) {
        lb += variable
      }
    }
    lb.toList
  }
}
