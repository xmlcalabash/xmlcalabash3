package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

import java.net.{URI, URISyntaxException}
import scala.xml.SAXParseException

object XdmLocation {
  // Can't use a constructor for this because node.getBaseURI might fail
  def from(node: XdmNode): Option[XdmLocation] = {
    val uristr = node.getUnderlyingNode.getBaseURI
    if (Option(uristr).isEmpty) {
      None
    } else {
      try {
        val buri = new URI(uristr)
        Some(new XdmLocation(node))
      } catch {
        case _: URISyntaxException =>
          throw XProcException.xdInvalidURI(uristr, None)
        case ex: Exception =>
          throw ex
      }
    }
  }
}

class XdmLocation private(private val puri: String, private val pline: Int, private val pcol: Int) extends Location {
  private var _uri: Option[String] = Some(puri)
  private var _line = pline
  private var _col = pcol

  private def this(node: XdmNode) = {
    this(Urify.urify(node.getBaseURI.toString), node.getLineNumber, node.getColumnNumber)

    // Special case: if there's a preceding sibling PI that contains the original
    // location, decode that and use it.
    var pi = Option.empty[XdmNode]
    var found = false
    val iter = node.axisIterator(Axis.PRECEDING_SIBLING)
    while (iter.hasNext) {
      val pnode = iter.next()
      if (!found) {
        pnode.getNodeKind match {
          case XdmNodeKind.TEXT =>
            if (pnode.getStringValue.trim != "") {
              found = true
            }
          case XdmNodeKind.PROCESSING_INSTRUCTION =>
            if (pnode.getNodeName.getLocalName == "_xmlcalabash") {
              pi = Some(pnode)
              found = true
            }
          case _ => found = true
        }
      }
    }

    if (found && pi.isDefined) {
      val str = pi.get.getStringValue
      val uripatn = ".*uri=\"([^\"]+)\".*".r
      val linepatn = ".*line=\"(\\d+)\".*".r
      val colpatn = ".*column=\"(\\d+)\".*".r

      str match {
        case uripatn(uri) =>
          _uri = Some(uri)
        case _ =>
          _uri = None
      }

      str match {
        case linepatn(line) =>
          _line = line.toInt
        case _ =>
          _line = -1
      }

      str match {
        case colpatn(col) =>
          _col = col.toInt
        case _ =>
          _col = -1
      }
    }
  }

  def this(ex: SAXParseException) = {
    this(ex.getSystemId, ex.getLineNumber, ex.getColumnNumber)
  }

  override def uri: Option[String] = _uri

  override def line: Option[Long] = {
    if (_line > 0) {
      Some(_line.toLong)
    } else {
      None
    }
  }

  override def column: Option[Long] = {
    if (_col > 0) {
      Some(_col.toLong)
    } else {
      None
    }
  }

  override def toString: String = {
    var str = ""
    if (_uri.isDefined) {
      str += _uri.get
    }
    if (line.isDefined) {
      str += ":" + line.get.toString
    }
    if (column.isDefined) {
      str += ":" + column.get.toString
    }
    str
  }
}
