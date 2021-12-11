package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime

import scala.collection.mutable

class XPipe(config: XMLCalabash) extends XDataSource(config) {
  private var _step = Option.empty[String]
  private var _port = Option.empty[String]

  private var _fromPort: XPort = _
  private var _fromStep: XArtifact = _
  private var _toStep: XArtifact = _

  def step: Option[String] = _step
  def port: Option[String] = _port
  def from: Option[XPort] = Option(_fromPort)

  def this(parent: XArtifact, step: Option[String], port: Option[String]) = {
    this(parent.config)
    this.parent = parent
    staticContext = parent.staticContext
    _synthetic = true
    _step = step
    _port = port
  }

  def this(parent: XArtifact, step: String, port: String) = {
    this(parent, Some(step), Some(port))
  }

  // This constructor works even if we don't subsequently attempt to validate the connection
  def this(parent: XArtifact, originPort: XPort) = {
    this(parent.config)
    this.parent = parent
    staticContext = parent.staticContext
    _synthetic = true
    _fromPort = originPort
    _fromStep = originPort.ancestorNode.get
    _step = Some(originPort.parent.get.asInstanceOf[XStep].stepName)
    _port = Some(originPort.port)
    _toStep = parent.ancestorNode.get
  }

  def this(port: XPort) = {
    this(port.config)
    val pstep = port.ancestorStep
    staticContext = port.staticContext
    _synthetic = true
    _step = Some(pstep.get.stepName)
    _port = Some(port.port)
  }

  def this(pipe: XPipe) = {
    this(pipe.config)
    staticContext = pipe.staticContext
    _synthetic = true
    _step = pipe._step
    _port = pipe._port
  }

  protected[xxml] def graphEdges(runtime: XMLCalabashRuntime): Unit = {
    // FIXME: make handling of dropped variables more robust
    var connect = true
    _toStep match {
      case bind: XNameBinding =>
        connect = bind.usedByPipeline
      case _ =>
        ()
    }

    if (connect) {
      val fromNode = runtime.node(_fromStep)
      val toNode = runtime.node(_toStep)

      parent.get match {
        case xport: XPort =>
          var sport = xport.port
          var rport = port.get

          if (sport == "#anon") {
            sport = "source"
            if (_toStep.ancestorOf(_fromStep)) {
              sport = "#anon_result"
            }
          }

          if (rport == "#anon") {
            rport = "#anon_result"
          }

          if (_fromStep eq _toStep) {
            // This is a special case that the Graph doesn't catch.
            // FIXME: fix this bug in the graph construction code

            val ps = _fromPort.parent.get
            ps match {
              case _: XContainer =>
                () // this is ok
              case _ =>
                throw XProcException.xsLoop(ps.asInstanceOf[XStep].stepName, rport, location)
            }
          }

          //println(s"Edge from ${_fromStep}/${rport} to ${_toStep}/${sport}")
          runtime.graph.addOrderedEdge(fromNode, rport, toNode, sport)
        case _: XNameBinding =>
          //println(s"Edge from ${_fromStep}/${port.get} to ${_toStep}/source")
          runtime.graph.addOrderedEdge(fromNode, port.get, toNode, "source")
      }
    }
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      if (attributes.contains(XProcConstants._step)) {
        _step = Some(staticContext.parseNCName(attr(XProcConstants._step).get))
      }
      if (attributes.contains(XProcConstants._port)) {
        _port = Some(staticContext.parseNCName(attr(XProcConstants._port).get))
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()
    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }
    allChildren = List()
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    initial
  }

  override protected[xxml] def elaboratePortConnections(): Unit = {
    if (step.isEmpty) {
      if (drp.isEmpty) {
        error(XProcException.xsPipeWithoutStepOrDrp(location))
      } else {
        drp.get.parent.get match {
          case xstep: XStep =>
            _step = Some(xstep.stepName)
          case _ =>
            error(XProcException.xiThisCantHappen("Parent of drp is not a step?"))
        }
      }
    }
  }

  override protected[xxml] def elaborateValidatePortConnections(ports: XPortBindingContext): Unit = {
    val gparent = if (parent.isDefined) {
      parent.get.parent
    } else {
      None
    }

    val internal = if (gparent.isDefined) {
      gparent.get match {
        case _: XChoose => true
        case _ => false
      }
    } else {
      false
    }

    val from = if (port.isEmpty) {
      ports.primaryPort(_step.get)
    } else {
      if (internal) {
        ports.privatePort(this)
      } else {
        ports.port(this)
      }
    }

    if (from.isEmpty) {
      if (port.isEmpty) {
        error(XProcException.xsPortNotReadableNoPrimaryInput(_step.get, location))
      } else {
        error(XProcException.xsPortNotReadable(_step.get, _port.get, location))
      }
      return
    }

    _fromPort = from.get
    _fromStep = from.get.ancestorNode.get
    _port = Some(_fromPort.port)
    _toStep = parent.get.ancestorNode.get
  }

  override protected[xxml] def computeReadsFrom(): Unit = {
    super.computeReadsFrom()
    val step = _fromPort.parent.get
    for (child <- step.children[XWithOutput]) {
      if (child.port == _fromPort.port) {
        child.readBy = _toStep
      }
    }
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("step", _step)
    attr.put("port", _port)
    dumpTree(sb, "p:pipe", attr.toMap)
  }

  override def toString: String = {
    s"from ${step.getOrElse("(drp step)")}/${port.getOrElse("(primary output)")}"
  }
}
