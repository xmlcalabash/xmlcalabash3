package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants, XValueParser}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XWithInput(config: XMLCalabash) extends XPort(config) {

  def this(parentStep: XArtifact, port: String) = {
    this(parentStep.config)
    parent = parentStep
    _synthetic = true
    _syntheticName = Some(XProcConstants.p_with_input)
    _port = port
    _primary = Some(true)
    _sequence = Some(true)
    _content_types = MediaType.MATCH_ANY
    staticContext = parentStep.staticContext
  }

  def this(parentStep: XArtifact, port: String, primary: Boolean, sequence: Boolean, contentTypes: List[MediaType]) = {
    this(parentStep.config)
    parent = parentStep
    _synthetic = true
    _syntheticName = Some(XProcConstants.p_with_input)
    _port = port
    _primary = Some(primary)
    _sequence = Some(sequence)
    _content_types = contentTypes
    staticContext = parentStep.staticContext
  }

  override protected[xxml] def port_=(port: String): Unit = {
    if (portSpecified) {
      throw XProcException.xiThisCantHappen("Attempt to change port on p:input")
    }
    _synthetic = true
    _syntheticName = Some(XProcConstants.p_with_input)
    _port = port
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      if (attributes.contains(XProcConstants._port)) {
        _port = staticContext.parseNCName(attr(XProcConstants._port).get)
      }
    } catch {
      case ex: XProcException =>
        error(ex)
    }

    if (_port != "#anon" && parent.isDefined) {
      parent.get match {
        case c: XForEach =>
          // FIXME: it's a weird sharp edge that the port has to be called 'source' in the graph
          if (_port != "source") {
            error(XProcException.xsPortNotAllowed(_port, c.name.getOrElse(c.tumble_id), location))
          }
        case c: XViewport =>
          error(XProcException.xsPortNotAllowed(_port, c.name.getOrElse(c.tumble_id), location))
        case c: XChoose =>
          error(XProcException.xsPortNotAllowed(_port, c.name.getOrElse(c.tumble_id), location))
        case c: XWhen =>
          error(XProcException.xsPortNotAllowed(_port, c.name.getOrElse(c.tumble_id), location))
        case c: XIf =>
          error(XProcException.xsPortNotAllowed(_port, c.name.getOrElse(c.tumble_id), location))
        case _ =>
          ()
      }
    }

    _select = attr(XProcConstants._select)
    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    val decl = stepDeclaration
    if (decl.isDefined) {
      if (decl.get.inputPorts.contains(port)) {
        val dinput = decl.get.input(port)
        primary = dinput.primary
        sequence = dinput.sequence
        contentTypes = dinput.contentTypes
      }
    } else {
      parent.get match {
        case _: XWhen =>
          _port = "condition"
        case _: XForEach =>
          primary = false
          sequence = true
          contentTypes = MediaType.MATCH_ANY
        case _ =>
          ()
      }
    }

    super.validate()
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    super.elaborateDefaultReadablePort(initial)
    if (!primary) {
      // If the input isn't primary, it doesn't get automatically joined to the DRP
      // Except for looping steps where the "current" port is primary for the inner steps
      // even though this port is primary for the loop itself. Sigh.
      parent.get match {
        case _: XLoopingStep =>
          ()
        case _ =>
          drp = None
      }
    }
    initial
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    super.elaborateNameBindings(initial);

    if (_select.isEmpty) {
      return initial
    }

    try {
      val parser = new XValueParser(config, staticContext, _select.get)

      val refs = parser.variables
      var static = parser.static

      val namepipe = ListBuffer.empty[XNameBinding]
      for (ref <- refs) {
        val cbind = initial.inScopeConstants.get(ref)
        val dbind = initial.inScopeDynamics.get(ref)
        if (cbind.isDefined) {
          // ok
        } else if (dbind.isDefined) {
          static = false
          dbind.get match {
            case v: XVariable =>
              namepipe += v
            case o: XOption =>
              namepipe += o
            case _ =>
              error(XProcException.xiThisCantHappen(s"Unexpected name binding: ${dbind.get}"))
          }
        } else {
          error(XProcException.xsNoBindingInExpression(ref, None))
        }
      }

      if (exceptions.nonEmpty) {
        return initial
      }

      if (static) {
        throw XProcException.xiThisCantHappen("Static binding in with-input unsupported")
      } else {
        if (namepipe.nonEmpty) {
          val xwi = new XWithInput(this, "#bindings", false, true, MediaType.MATCH_ANY)
          for (binding <- namepipe) {
            val xstep = initial.inScopeDynamics.get(binding.name)
            val pipe = new XPipe(xwi, xstep.get.tumble_id, "result")
            xwi.addChild(pipe)
          }
          _selectBindings = Some(xwi)
        }
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }

    initial
  }

  override protected[xxml] def elaboratePortConnections(): Unit = {
    if (drp.isDefined) {
      if (children[XDataSource].isEmpty && port != "#bindings") {
        val pipe = new XPipe(drp.get)
        addChild(pipe)
      }
    } else {
      if (children[XDataSource].isEmpty && primary) {
        if (parent.isDefined) {
          parent.get match {
            case _: XWhen =>
              // Can't raise this statically in case it doesn't actually arise
              logger.debug("The p:when expression will fail dynamically because no context item is available")
            case step: XAtomicStep =>
              val decl = step.stepDeclaration.get
              val input = decl.input(port)
              if (input.defaultInputs.nonEmpty) {
                for (ds <- input.defaultInputs) {
                  ds match {
                    case inline: XInline =>
                      inline.staticContext = inline.staticContext.withConstants(input.staticContext.constants)
                      addChild(new XInline(this, inline))
                    case doc: XDocument =>
                      doc.staticContext = doc.staticContext.withConstants(input.staticContext.constants)
                      addChild(new XDocument(this, doc))
                    case _: XEmpty =>
                      addChild(new XEmpty(this))
                    case _ =>
                      throw XProcException.xiThisCantHappen(s"Default inputs contain ${ds}")
                  }
                }
              } else {
                throw XProcException.xsUnconnectedPrimaryInputPort(step.name.getOrElse(step.tumble_id), port, location)
              }
            case step: XStep =>
              throw XProcException.xsUnconnectedPrimaryInputPort(step.name.getOrElse(step.tumble_id), port, location)
            case _ =>
              throw XProcException.xiThisCantHappen("Parent of p:with-input is not a step?")
          }
        } else {
          throw XProcException.xiThisCantHappen("A p:with-input has no parent?")
        }
      }
    }

    super.elaboratePortConnections()
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("port", Some(_port))
    attr.put("select", _select)
    attr.put("primary", _primary)
    attr.put("sequence", _sequence)

    if (drp.isDefined) {
      attr.put("drp", Some(drp.get.tumble_id))
    }

    dumpTree(sb, "p:with-input", attr.toMap)
  }
}
