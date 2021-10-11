package com.xmlcalabash.runtime

class XProcExpression(val context: StaticContext, val extensionFunctionsAllowed: Boolean) {
  def this(context: StaticContext) = {
    this(context, false)
  }

  override def toString: String = "{XProcExpression}"
}
