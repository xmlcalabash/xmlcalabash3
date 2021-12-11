package com.xmlcalabash.util

import net.sf.saxon.s9api.XdmValue

class XProcVarValue(val value: XdmValue, val context: MinimalStaticContext) {
  def getStringValue: String = {
    val iter = value.iterator()
    var s = ""
    while (iter.hasNext) {
      s += iter.next.getStringValue
    }
    s
  }
}
