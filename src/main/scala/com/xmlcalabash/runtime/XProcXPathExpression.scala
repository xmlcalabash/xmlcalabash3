package com.xmlcalabash.runtime

import com.xmlcalabash.runtime.params.XPathBindingParams
import net.sf.saxon.s9api.{SequenceType, XdmAtomicValue}

class XProcXPathExpression(override val context: StaticContext,
                           val expr: String,
                           val as: Option[SequenceType],
                           val values: Option[List[XdmAtomicValue]],
                           val params: Option[XPathBindingParams])
  extends XProcExpression(context) {

  def this(context: StaticContext, expr: String) = {
    this(context, expr, None, None, None)
  }

  def this(context: StaticContext, expr: String, as: Option[SequenceType]) = {
    this(context, expr, as, None, None)
  }

  def this(context: StaticContext, expr: String, as: Option[SequenceType], values: Option[List[XdmAtomicValue]], params: XPathBindingParams) = {
    this(context, expr, as, values, Some(params))
  }

  override def toString: String = expr
}

