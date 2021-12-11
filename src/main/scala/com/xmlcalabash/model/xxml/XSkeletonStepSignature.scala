package com.xmlcalabash.model.xxml

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XSkeletonStepSignature(val decl: XDeclareStep) {
  private val _inputs = mutable.HashSet.empty[String]
  private val _outputs = mutable.HashSet.empty[String]
  private val _options = ListBuffer.empty[QName]

  private val typeAttr = decl.attributes.get(XProcConstants._type)
  if (typeAttr.isEmpty) {
    throw XProcException.xiThisCantHappen("Attempt to create skeleton step signature without type")
  }

  private val _stepType = decl.staticContext.parseQName(typeAttr.get)

  for (child <- decl.allChildren) {
    child match {
      case _: XInput =>
        if (child.attributes.contains(XProcConstants._port)) {
          _inputs += child.attributes(XProcConstants._port)
        }
      case _: XOutput =>
        if (child.attributes.contains(XProcConstants._port)) {
          _outputs += child.attributes(XProcConstants._port)
        }
      case _: XOption =>
        if (child.attributes.contains(XProcConstants._name)) {
          _options += decl.staticContext.parseQName(child.attributes(XProcConstants._name))
        }
      case _ =>
        ()
    }
  }

  def stepType: QName = _stepType
  def inputs: Set[String] = _inputs.toSet
  def outputs: Set[String] = _outputs.toSet
  def options: List[QName] = _options.toList
}
