package com.xmlcalabash.util

import net.sf.saxon.s9api.{Axis, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}

object ValueUtils {
  def singletonStringValue(value: XdmValue): String = {
    if (value.size() == 1) {
      value.itemAt(0).getStringValue
    } else {
      throw new RuntimeException("No sequences.")
    }
  }

  def stringValue(value: XdmValue): String = {
    var s = ""
    val iter = value.iterator()
    while (iter.hasNext) {
      s += iter.next.getStringValue
    }
    s
  }

  def isTrue(value: Option[Any]): Boolean = {
    if (value.isEmpty) {
      return false
    }

    value.get match {
      case string: String => List("1", "true", "yes").contains(string)
      case atomic: XdmAtomicValue => List("1", "true", "yes").contains(atomic.getStringValue)
      case _ => false
    }
  }
}
