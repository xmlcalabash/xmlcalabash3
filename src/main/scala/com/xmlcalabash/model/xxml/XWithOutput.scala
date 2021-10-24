package com.xmlcalabash.model.xxml

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XWithOutput(parart: XArtifact, port: String) extends XPort(parart.config) {
  private val _readBy = ListBuffer.empty[XArtifact]

  _synthetic = true
  staticContext = parart.staticContext
  this.parent = parart
  _port = port

  protected[xxml] def readBy: List[XArtifact] = _readBy.toList
  protected[xxml] def readBy_=(art: XArtifact): Unit = {
    _readBy += art
  }

  override protected[xxml] def checkAttributes(): Unit = {
    // it's synthetic
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    val decl = stepDeclaration

    if (decl.isEmpty) {
      parent.get match {
        case atomic: XAtomicStep =>
          if (atomic.stepType == XProcConstants.cx_document_loader
            || atomic.stepType == XProcConstants.cx_inline_loader) {
            _primary = Some(true)
            _sequence = Some(false)
            _content_types = MediaType.MATCH_ANY.toList
          } else {
            error(XProcException.xiThisCantHappen("Parent of with-output isn't a cx: loader?"))
          }
        case _ =>
          error(XProcException.xiThisCantHappen("Grandparent of with-output isn't an atomic step?"))
      }

      return
    }

    if (decl.isDefined && decl.get.outputPorts.contains(port)) {
      val doutput = decl.get.output(port)
      primary = doutput.primary
      sequence = doutput.sequence
      contentTypes = doutput.contentTypes
    }

    super.validate()
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("port", Some(_port))
    attr.put("primary", _primary)
    attr.put("sequence", _sequence)
    dumpTree(sb, "p:with-output", attr.toMap)
  }
}
