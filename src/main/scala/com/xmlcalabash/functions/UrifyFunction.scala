package com.xmlcalabash.functions

import com.xmlcalabash.XMLCalabash
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

class UrifyFunction(runtime: XMLCalabash) extends FunctionImpl() {
  val funcname = new StructuredQName("p", XProcConstants.ns_p, "urify")

  override def getFunctionQName: StructuredQName = funcname

  override def getMinimumNumberOfArguments: Int = 1

  override def getMaximumNumberOfArguments: Int = 2

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
      if (arguments.length > 1 && Option(arguments(1).head).isDefined) {
        new StringValue(Urify.urify(relativeuri, arguments(1).head.getStringValue))
      } else {
        new StringValue(Urify.urify(relativeuri))
      }
    }
  }
}
