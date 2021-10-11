package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{AnyURIValue, AtomicValue, SequenceType}

class Cwd(runtime: XMLCalabashConfig) extends FunctionImpl {
  val funcname = new StructuredQName("exf", XProcConstants.ns_exf, "cwd")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.EMPTY_SEQUENCE)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

  override def makeCallExpression(): ExtensionFunctionCall = {
    new CwdCall(this)
  }

  private class CwdCall (funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext == null) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val cwd = runtime.staticBaseURI.toASCIIString
      new AnyURIValue(cwd)
    }
  }
}
