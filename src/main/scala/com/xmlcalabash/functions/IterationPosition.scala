package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{Int64Value, QNameValue, SequenceType}

class IterationPosition(runtime: XMLCalabashConfig) extends FunctionImpl {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "iteration-position")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array()

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

  override def makeCallExpression(): ExtensionFunctionCall = {
    new Call(this)
  }

  private class Call(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext == null) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val dynContext = exprEval.dynContext
      new Int64Value(dynContext.get.iterationPosition)
    }
  }
}
