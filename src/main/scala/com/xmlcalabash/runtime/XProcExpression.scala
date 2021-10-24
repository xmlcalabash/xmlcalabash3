package com.xmlcalabash.runtime

import com.xmlcalabash.util.MinimalStaticContext

class XProcExpression(val context: MinimalStaticContext, val extensionFunctionsAllowed: Boolean) {
  def this(context: MinimalStaticContext) = {
    this(context, false)
  }

  override def toString: String = "{XProcExpression}"
}
