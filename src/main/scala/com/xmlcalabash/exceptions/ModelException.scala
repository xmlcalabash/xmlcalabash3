package com.xmlcalabash.exceptions

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.ExceptionCode.ExceptionCode
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XProcLocation
import net.sf.saxon.s9api.{QName, XdmNode}

class ModelException(val code: ExceptionCode, val data: List[String], private val loc: Option[Location]) extends Exception {
  private var _location = Option.empty[Location]

  _location = loc

  def location: Option[Location] = _location

  def this(code: ExceptionCode, data: List[String], node: XdmNode) = {
    this(code, data, None)
    _location = Some(new XProcLocation(node))
  }

  def this(code: ExceptionCode, data: String, node: XdmNode) = {
    this(code, List(data), node)
  }

  def this(code: ExceptionCode, data: String, loc: Option[Location]) = {
    this(code, List(data), loc)
  }

  def this(code: ExceptionCode, data: String, loc: Location) = {
    this(code, List(data), Some(loc))
  }

  def this(code: ExceptionCode, data: List[String], loc: Location) = {
    this(code, data, Some(loc))
  }

  def message: String = {
    code match {
      case ExceptionCode.DUPINPUTSIG => s"Duplicate input port name specified in step signature: ${data.head}"
      case ExceptionCode.DUPOUTPUTSIG => s"Duplicate output port name specified in step signature: ${data.head}"
      case ExceptionCode.DUPOPTSIG => s"Duplicate output port name specified in step signature: ${data.head}"
      case ExceptionCode.BADINPUTSIG =>
        val stepType = data.head
        val port = data(1)
        s"Step $stepType has no input port named $port"
      case ExceptionCode.BADOUTPUTSIG =>
        val stepType = data.head
        val port = data(1)
        s"Step $stepType has no output port named $port"
      case ExceptionCode.BADOPTSIG =>
        val stepType = data.head
        val optName = data(1)
        s"Step $stepType has no option named $optName"
      case ExceptionCode.NOTYPE => s"Unknown step type: ${data.head}"
      case ExceptionCode.NOIMPL => s"No implementation is defined for step type: ${data.head}"
      case ExceptionCode.IMPLNOTSTEP => s"The implementation of ${data.head} is not a step"
      case ExceptionCode.BADTREENODE =>
        val nodeKind = data.head
        val nodeName = data(1)
        if (nodeKind == "ELEMENT") {
          s"Internal error: tree construction failed: unexpected element: $nodeName"
        } else {
          s"Internal error: tree construction failed: unexpected node kind: $nodeKind"
        }
      case ExceptionCode.BADBOOLEAN => s"Value is not boolean: ${data.head}"
      case ExceptionCode.NOPREFIX => s"Prefix has no in-scope namespace binding: ${data.head}"
      case ExceptionCode.BADATTR => s"Attribute not allowed here: ${data.head}"
      case ExceptionCode.BADATOMICATTR => s"Unqualified attribute not allowed on atomic step: ${data.head}"
      case ExceptionCode.BADATOMICINPUTPORT =>
        val stepType = data.head
        val port = data(1)
        s"Atomic $stepType steps have no input port named $port"
      case ExceptionCode.BADATOMICOUTPUTPORT =>
        val stepType = data.head
        val port = data(1)
        s"Atomic $stepType steps have no output port named $port"
      case ExceptionCode.BADCONTAINERATTR => s"Unqualified attribute not allowed: ${data.head}"
      case ExceptionCode.BADPIPE => s"The `p:pipe' element is not allowed here: ${data.head}"
      case ExceptionCode.BADSEQ => s"Attribute not allowed here: ${data.head}"
      case ExceptionCode.BADPRIMARY => s"Attribute not allowed here: ${data.head}"
      case ExceptionCode.NAMEATTRREQ => s"The `name' attribute is required on ${data.head}"
      case ExceptionCode.SELECTATTRREQ => s"The `select' attribute is required on ${data.head}"
      case ExceptionCode.BADPIPELINEROOT => s"Invalid root element for pipeline: ${data.head}"
      case ExceptionCode.INVALIDPIPELINE => s"The pipeline is invalid"
      case ExceptionCode.NOTASTEP => s"Element does not appear to be a step: ${data.head}"
      case ExceptionCode.BADSERPORT => s"Attempt to set serialization options for non-existant port: ${data.head}"
      case ExceptionCode.BADSERSTANDALONE => s"Invalid standalone option on serialization: ${data.head}"
      case ExceptionCode.DUPCONTAINERINPUTPORT => s"Input port already exist: ${data.head}"
      case ExceptionCode.DUPCONTAINEROUTPUTPORT => s"Output port already exists: ${data.head}"
      case ExceptionCode.DUPPRIMARYINPUT =>
        val newport = data.head
        val curport = data(1)
        s"Step already has primary input port: $curport; cannot make $newport primary"
      case ExceptionCode.DUPPRIMARYOUTPUT =>
        val newport = data.head
        val curport = data(1)
        s"Step already has primary output port: $curport; cannot make $newport primary"
      case ExceptionCode.NOCONTAINEROUTPUT => s"No output connection for port: ${data.head}"
      case ExceptionCode.ATTRREQ => s"Required attribute is missing: ${data.head}"
      case ExceptionCode.BADAVT =>
        val name = data.head
        val expr = data(1)
        s"XPath syntax error in attribute value template for $name: $expr"
      case ExceptionCode.NOBINDING => s"Expression references variable not in-scope: ${data.head}"
      case ExceptionCode.DUPOTHERWISE => s"A p:choose can have at most one p:otherwise"
      case ExceptionCode.MISSINGWHEN => s"A p:choose must have at least one p:when or p:otherwise"
      case ExceptionCode.BADCHOOSECHILD => s"A p:choose cannot contain ${data.head}"
      case ExceptionCode.DIFFPRIMARYINPUT => s"Conflicting primary input port names: ${data.head} and ${data(1)}"
      case ExceptionCode.DIFFPRIMARYOUTPUT => s"Conflicting primary output port names: ${data.head} and ${data(1)}"
      case ExceptionCode.TESTREQUIRED => s"A test attribute is required on p:when"
      case ExceptionCode.DUPGROUP => s"A p:try must have exactly one p:group"
      case ExceptionCode.DUPFINALLY => s"A p:try must have at most one p:finally"
      case ExceptionCode.MISSINGGROUP => s"A p:try must have exactly one p:group"
      case ExceptionCode.MISSINGCATCH => s"A p:try must have at least one p:catch"
      case ExceptionCode.BADTRYCHILD => s"A p:try cannot contain ${data.head}"
      case ExceptionCode.INVALIDNAME => s"Invalid step name: ${data.head}"
      case ExceptionCode.NODRP => "No step or port specified and no default readable port exists"
      case ExceptionCode.MIXEDPIPE => s"You cannot specify connections with a pipe attribute and sub-elements"
      case ExceptionCode.NOSTEP => s"No step named ${data.head} is in scope"
      case ExceptionCode.NOPORT => s"Step named ${data.head} has no port named ${data(1)}"
      case ExceptionCode.NOPRIMARYINPUTPORT => s"Step ${data.head} has no primary input port for defaulted input"
      case ExceptionCode.DUPINPUTPORT => s"Duplicated input port name: ${data.head}"

      case ExceptionCode.INTERNAL => data.head
      case _ => "INTERNAL ERROR: No message for $code"
    }
  }

  def exceptionQName: QName = {
    code match {
      case ExceptionCode.DUPCONTAINEROUTPUTPORT => XProcException.staticErrorCode(11)
      case ExceptionCode.NAMEATTRREQ => XProcException.staticErrorCode(38)
      case ExceptionCode.NOPRIMARYINPUTPORT => XProcException.staticErrorCode(65)
      case ExceptionCode.DUPINPUTPORT => XProcException.staticErrorCode(86)
      case _ => new QName(XProcConstants.ns_cx, "ERROR")
    }
  }

  override def toString: String = {
    val loc = if (location.isDefined) {
      location.get.toString + ":"
    } else {
      ""
    }

    s"$loc$message"
  }
}
