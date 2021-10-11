package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{URIUtils, Urify}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.functions.AccessorFn.Component
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Item, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmEmptySequence, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.tree.iter.ArrayIterator
import net.sf.saxon.tree.tiny.{TinyDocumentImpl, TinyElementImpl, TinyTextImpl}
import net.sf.saxon.value.{QNameValue, SequenceType, StringValue}

class UrifyFunction(runtime: XMLCalabashConfig) extends FunctionImpl() {
  val funcname = new StructuredQName("p", XProcConstants.ns_p, "urify")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM, SequenceType.OPTIONAL_ITEM)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new UrifyCall(this)
  }

  private class UrifyCall(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    private var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      super.supplyStaticContext(context, locationId, arguments)
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val relativeuri = arguments(0).head.getStringValue
      val proposedbase = arguments(1).head.getStringValue

      if (proposedbase == "") {
        throw XProcException.xdUrifyFailed(relativeuri, proposedbase, None)
      }

      val result = new Urify(proposedbase).resolve(relativeuri)
      new StringValue(result.toString)
    }
  }
}
