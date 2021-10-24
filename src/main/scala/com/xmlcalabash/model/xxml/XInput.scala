package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XInput(config: XMLCalabash) extends XPort(config) {
  // FIXME: support select with variables on p:input

  def this(step: XContainer, port: Option[String]) = {
    this(step.config)
    staticContext = step.staticContext
    synthetic = true
    syntheticName = XProcConstants.p_input
    parent = step
    if (port.isDefined) {
      _port = port.get
    }
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      parent.get match {
        case loop: XLoopingStep =>
          if (attributes.contains(XProcConstants._port)) {
            error(XProcException.xsPortNotAllowed(attributes(XProcConstants._port), loop.stepName, location))
          }
        case _ =>
          if (attributes.contains(XProcConstants._port)) {
            _port = staticContext.parseNCName(attr(XProcConstants._port)).get
          } else {
            error(XProcException.xsMissingRequiredAttribute(XProcConstants._port, None))
          }
      }

      _sequence = staticContext.parseBoolean(attr(XProcConstants._sequence))
      _primary = staticContext.parseBoolean(attr(XProcConstants._primary))
      _select = attr(XProcConstants._select)

      _content_types = staticContext.parseContentTypes(attr(XProcConstants._content_types))
      if (_content_types.isEmpty) {
        _content_types = List(MediaType.OCTET_STREAM)
      }

      _href = attr(XProcConstants._href)
      // pipe isn't allowed on p:input
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  override protected[xxml] def checkEmptyAttributes(): Unit = {
    super.checkEmptyAttributes()
    _attrChecked = true
  }

  override protected[xxml] def validate(): Unit = {
    if (!_attrChecked) {
      checkAttributes()
      checkEmptyAttributes()
    }

    super.validate()
  }

  protected[xxml] def checkDefaultInputs(): Unit = {
    // Any children of a p:input in this case are just default bindings;
    // squirrel them away in case we need them later
    if (_pipe.isDefined) {
      throw XProcException.xsBadAttribute(XProcConstants._pipe, location)
    }
    if (children[XPipe].nonEmpty) {
      throw XProcException.xsElementNotAllowed(XProcConstants.p_pipe, location)
    }

    for (child <- allChildren) {
      child match {
        case _: XDocumentation => ()
        case _: XPipeinfo => ()
        case ds: XDataSource =>
          if (_href.isDefined) {
            throw XProcException.xsHrefAndOtherSources(child.location)
          }
          ds.validate()
          _defaultInputs += ds
        case _ =>
          throw XProcException.xiThisCantHappen("Default p:input contains something that isn't an XDataSource?")
      }
    }

    if (_href.isDefined) {
      val doc = new XDocument(this, _href.get)
      _defaultInputs += doc
      _href = None
    }

    allChildren = List()
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    super.elaborateDefaultReadablePort(initial)
    if (primary) {
      Some(this)
    } else {
      initial
    }
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    super.elaborateNameBindings(initial)
    for (child <- defaultInputs) {
      child.elaborateNameBindings(initial)
    }
    initial
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("port", Some(_port))
    attr.put("select", _select)
    attr.put("primary", _primary)
    attr.put("sequence", _sequence)
    dumpTree(sb, "p:input", attr.toMap)
  }
}
