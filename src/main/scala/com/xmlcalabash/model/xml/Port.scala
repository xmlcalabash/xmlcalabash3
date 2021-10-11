package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.util.MediaType

import scala.collection.mutable.ListBuffer

class Port(override val config: XMLCalabashConfig) extends Artifact(config) {
  protected var _port = ""
  protected[xml] var _sequence = Option.empty[Boolean]
  protected[xml] var _primary = Option.empty[Boolean]
  protected[xml] var _select = Option.empty[String]
  protected[xml] var _content_types = List.empty[MediaType]

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  def port: String = _port
  protected[model] def port_=(port: String): Unit = {
    _port = port
  }
  def sequence: Boolean = _sequence.getOrElse(false)
  protected[model] def sequence_=(seq: Boolean): Unit = {
    _sequence = Some(seq)
  }
  def primary: Boolean = _primary.getOrElse(false)
  protected[model] def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }

  def select: Option[String] = _select

  def contentTypes: List[MediaType] = _content_types
  def contentTypes_=(types: List[MediaType]): Unit = {
    _content_types = types
  }

  def step: NamedArtifact = {
    if (parent.isDefined) {
      parent.get match {
        case art: NamedArtifact => art
        case _ => throw new RuntimeException("parent of port isn't a named artifact?")
      }
    } else {
      throw new RuntimeException("port has no parent?")
    }
  }

  def bindings: List[DataSource] = {
    val lb = ListBuffer.empty[DataSource]
    for (child <- allChildren) {
      child match {
        case ds: DataSource => lb += ds
        case _ => ()
      }
    }
    lb.toList
  }

  protected[model] def examineBindings(): Unit = {
    if (_href.isDefined && _pipe.isDefined) {
      throw XProcException.xsPipeAndHref(location)
    }

    if (_href.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsHrefAndOtherSources(location)
    }

    if (_pipe.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsPipeAndOtherSources(location)
    }

    if (_href.isDefined) {
      val doc = new Document(config)
      doc.staticContext = staticContext
      doc.href = _href.get
      addChild(doc)
    }

    if (_pipe.isDefined) {
      for (shortcut <- _pipe.get.split("\\s+")) {
        var port = Option.empty[String]
        var step = Option.empty[String]
        if (shortcut.contains("@")) {
          val re = "(.*)@(.*)".r
          shortcut match {
            case re(pname, sname) =>
              if (pname != "") {
                port = Some(pname)
              }
              if (sname == "") {
                throw XProcException.xsInvalidPipeToken(shortcut, location)
              }
              step = Some(sname)
          }
        } else {
          if (shortcut.trim() != "") {
            port = Some(shortcut)
          }
        }

        val pipe = new Pipe(config)
        if (step.isDefined) {
          pipe.step = step.get
        }
        if (port.isDefined) {
          pipe.port = port.get
        }
        addChild(pipe)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }

    var empty = false
    var nonEmpty = false
    var pns = false
    var implinline = false
    for (child <- allChildren) {
      child match {
        case _: Document =>
          nonEmpty = true
          pns = true
        case _: Empty =>
          empty = true
          pns = true
        case source: Inline =>
          nonEmpty = true
          if (source.synthetic) {
            implinline = true
          } else {
            pns = true
          }
        case _: Pipe =>
          nonEmpty = true
          pns = true
        case _: NamePipe => ()
        case _ => throw new RuntimeException(s"Unexpected port binding: $child")
      }
    }

    if (empty && nonEmpty) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }

    if (pns && implinline) {
      throw XProcException.xsInvalidPipeline("Cannot combine implicit inlines with elements from the p: namespace", location)
    }
  }
}
