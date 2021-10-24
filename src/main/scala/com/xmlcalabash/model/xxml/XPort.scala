package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType

import scala.collection.mutable.ListBuffer

abstract class XPort(config: XMLCalabash) extends XArtifact(config) {
  protected var _port: String = "#anon"
  protected var _sequence = Option.empty[Boolean]
  protected var _primary = Option.empty[Boolean]
  protected var _select = Option.empty[String]
  protected var _selectBindings = Option.empty[XWithInput]
  protected var _content_types = List.empty[MediaType]
  protected val _defaultInputs: ListBuffer[XDataSource] = ListBuffer.empty[XDataSource]
  // XInput and XOutput on p:declare-step are checked when the step API is
  // computed. Elsewhere, they need to be checked during validation.
  protected var _attrChecked = false
  private var _irrelevant = false

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  private var _drp: Option[XPort] = None

  def portSpecified: Boolean = !_port.startsWith("#")

  def port: String = _port
  protected[xxml] def port_=(name: String): Unit = {
    _port = name
  }
  def sequence: Boolean = _sequence.getOrElse(false)
  protected[xxml] def sequence_=(seq: Boolean): Unit = {
    _sequence = Some(seq)
  }
  def primary: Boolean = _primary.getOrElse(false)
  def primarySpecified: Boolean = _primary.nonEmpty
  protected[xxml] def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }
  def select: Option[String] = _select
  protected[xxml] def selectBindings: Option[XWithInput] = _selectBindings
  def contentTypes: List[MediaType] = _content_types
  protected[xxml] def contentTypes_=(types: List[MediaType]): Unit = {
    _content_types = types
  }

  def irrelevant: Boolean = _irrelevant
  protected[xxml] def irrelevant_=(irrelevant: Boolean): Unit = {
    _irrelevant = irrelevant
  }

  def defaultInputs: List[XDataSource] = _defaultInputs.toList

  protected[xxml] def drp: Option[XPort] = _drp
  protected[xxml] def drp_=(port: Option[XPort]): Unit = {
    _drp = port
  }

  protected[xxml] def graphEdges(runtime: XMLCalabashRuntime): Unit = {
    for (child <- allChildren) {
      child match {
        case pipe: XPipe =>
          pipe.graphEdges(runtime)
        case _ =>
          throw XProcException.xiThisCantHappen("Port has a child that isn't a pipe?")
      }
    }
  }

  override protected[xxml] def validate(): Unit = {
    allChildren = validateExplicitConnections(_href, _pipe)
    _href = None
    _pipe = None
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    super.elaborateDefaultReadablePort(initial)
  }

  override def toString: String = {
    if (parent.isDefined) {
      s"${parent.get} :: ${port}"
    } else {
      port
    }
  }
}
