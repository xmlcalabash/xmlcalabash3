package com.xmlcalabash.runtime.params

import com.jafpl.graph.BindingParams
import net.sf.saxon.s9api.{QName, XdmValue}

object XPathBindingParams {
  private val _empty = new XPathBindingParams(Map.empty[QName,XdmValue], false)
  def EMPTY: XPathBindingParams = _empty
}

class XPathBindingParams(val statics: Map[QName, XdmValue], val collection: Boolean) extends BindingParams {
  def this(collection: Boolean) = {
    this(Map.empty[QName,XdmValue], collection)
  }
  def this(statics: Map[QName,XdmValue]) = {
    this(statics, false)
  }
}
