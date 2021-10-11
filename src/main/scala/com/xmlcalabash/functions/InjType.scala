package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{QNameValue, SequenceType, StringValue}

class InjType(runtime: XMLCalabashConfig) extends FunctionImpl {
  private val funcname = new StructuredQName("cx", XProcConstants.ns_cx, "step-type")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.EMPTY_SEQUENCE)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_QNAME

  override def makeCallExpression(): ExtensionFunctionCall = {
    new Call(this)
  }

  private class Call(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext == null) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      if (exprEval.dynContext.get.injType.isDefined) {
        val qn = exprEval.dynContext.get.injType.get
        new QNameValue(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName)
      } else {
        throw XProcException.xiNotInInjectable()
      }
    }
  }
}
