package com.xmlcalabash.runtime

import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.{SequenceType, XdmAtomicValue}

class XProcXPathExpression(override val context: MinimalStaticContext,
                           val expr: String,
                           val as: Option[SequenceType],
                           val values: Option[List[XdmAtomicValue]],
                           val params: Option[XPathBindingParams])
  extends XProcExpression(context) {

  def this(context: MinimalStaticContext, expr: String) = {
    this(context, expr, None, None, None)
  }

  def this(context: MinimalStaticContext, expr: String, as: Option[SequenceType]) = {
    this(context, expr, as, None, None)
  }

  def this(context: MinimalStaticContext, expr: String, as: Option[SequenceType], values: Option[List[XdmAtomicValue]], params: XPathBindingParams) = {
    this(context, expr, as, values, Some(params))
  }

  override def toString: String = expr
}

