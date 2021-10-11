package com.xmlcalabash.runtime

import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{SequenceType, XdmAtomicValue}

class XProcXPathValue(override val context: StaticContext,
                      val value: XProcVarValue,
                      val as: Option[SequenceType],
                      val values: Option[List[XdmAtomicValue]],
                      val params: Option[XPathBindingParams])
  extends XProcExpression(context) {

  def this(context: StaticContext, value: XProcVarValue, as: Option[SequenceType], values: Option[List[XdmAtomicValue]], params: XPathBindingParams) = {
    this(context, value, as, values, Some(params))
  }

  override def toString: String = value.toString
}

