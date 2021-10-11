package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.functions.AccessorFn.Component
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Item, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmEmptySequence, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.tree.iter.ArrayIterator
import net.sf.saxon.tree.tiny.{TinyDocumentImpl, TinyElementImpl, TinyTextImpl}
import net.sf.saxon.value.{QNameValue, SequenceType, StringValue}

class DocumentProperty(runtime: XMLCalabashConfig) extends FunctionImpl() {
  val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-property")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM, SequenceType.SINGLE_ITEM)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.OPTIONAL_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropCall(this)
  }

  private class DocPropCall(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
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

      val item = arguments(0).head
      val propname = arguments(1).head

      val itemBaseUri = item match {
        case i: TinyDocumentImpl => Option(i.getBaseURI)
        case i: TinyElementImpl => Option(i.getBaseURI)
        case i: TinyTextImpl => Option(i.getBaseURI)
        case _ => None
      }

      val msg = getMessage(item, exprEval)
      val prop: QName = propname match {
        case pval: QNameValue =>
          new QName(pval.getComponent(Component.NAMESPACE).getStringValue, pval.getComponent(Component.LOCALNAME).getStringValue)
        case sval: StringValue =>
          val colonName = sval.getStringValue
          if (colonName.contains(":")) {
            val pfx = colonName.substring(0, colonName.indexOf(":"))
            val local = colonName.substring(colonName.indexOf(":") + 1)
            val uri = Option(staticContext.getNamespaceResolver.getURIForPrefix(pfx, false))
            if (uri.isDefined) {
              new QName(pfx, uri.get, local)
            } else {
              throw XProcException.xdKeyIsInvalidQName(colonName, None)
            }
          } else {
            new QName("", colonName)
          }
        case _ =>
          throw new RuntimeException("Unexected argument to document property")
      }

      if (msg.isEmpty) {
        XdmEmptySequence.getInstance().getUnderlyingValue
      } else {
        val properties: Map[QName, XdmValue] = msg.get match {
          case item: XProcItemMessage =>
            item.metadata.properties
          case _ =>
            Map.empty[QName, XdmItem]
        }

        if (properties.contains(prop)) {
          properties(prop) match {
            case node: XdmNode =>
              node.getUnderlyingNode
            case atomic: XdmItem =>
              // I wonder if there's a better way?
              val iter = new ArrayIterator[Item](Array(atomic.getUnderlyingValue))
              iter.materialize()
            case _ =>
              XdmEmptySequence.getInstance().getUnderlyingValue
          }
        } else {
          if (prop == XProcConstants._base_uri && itemBaseUri.isDefined) {
            // I wonder if there's a better way?
            val iter = new ArrayIterator[Item](Array(new XdmAtomicValue(itemBaseUri.get).getUnderlyingValue))
            iter.materialize()
          } else {
            XdmEmptySequence.getInstance().getUnderlyingValue
          }
        }
      }
    }
  }
}