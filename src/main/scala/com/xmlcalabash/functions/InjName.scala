package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{SequenceType, StringValue}

class InjName(runtime: XMLCalabashConfig) extends FunctionImpl {
  private val funcname = new StructuredQName("cx", XProcConstants.ns_cx, "step-name")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.EMPTY_SEQUENCE)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_STRING

  override def makeCallExpression(): ExtensionFunctionCall = {
    new Call(this)
  }

  private class Call(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext == null) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      if (exprEval.dynContext.get.injName.isDefined) {
        new StringValue(exprEval.dynContext.get.injName.get)
      } else {
        throw XProcException.xiNotInInjectable()
      }
    }
  }
}
