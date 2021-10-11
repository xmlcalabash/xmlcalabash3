package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import net.sf.saxon.tree.tiny.{TinyDocumentImpl, TinyElementImpl, TinyTextImpl}
import net.sf.saxon.value.{SequenceType, StringValue}

class DocumentProperties(runtime: XMLCalabashConfig) extends FunctionImpl {
  val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropsCall(this)
  }

  private class DocPropsCall(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
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

      val item = arguments(0).head()
      val msg = getMessage(item, exprEval)

      val itemBaseUri = item match {
        case i: TinyDocumentImpl => Option(i.getBaseURI)
        case i: TinyElementImpl => Option(i.getBaseURI)
        case i: TinyTextImpl => Option(i.getBaseURI)
        case _ => None
      }

      var map = new XdmMap()

      if (msg.isDefined) {
        val props: Map[QName,XdmValue] = msg.get match {
          case item: XProcItemMessage =>
            item.metadata.properties
          case _ =>
            Map.empty[QName,XdmItem]
        }

        for (key <- props.keySet) {
          val value = props(key)
          key match {
            case XProcConstants._base_uri =>
              if (itemBaseUri.isDefined) {
                map = map.put(new XdmAtomicValue(XProcConstants._base_uri), new XdmAtomicValue(itemBaseUri.get))
              } else {
                map = map.put(new XdmAtomicValue(XProcConstants._base_uri), value)
              }
            case _ =>
              if (key.getNamespaceURI == "") {
                map = map.put(new XdmAtomicValue(key.getLocalName), value)
              } else {
                map = map.put(new XdmAtomicValue(key), value)
              }
          }
        }

        map.getUnderlyingValue
      } else {
        logger.debug("p:document-properties found no match for argument")
        // We're going to have to try to fake this. It happens when someone, for example,
        // evaluates p:document-properties(doc('foo.xml')). Because the doc function is
        // evaluated in the expression and never passes through XProc, we don't have a
        // property mapping for it.
        if (itemBaseUri.isDefined) {
          map = map.put(new XdmAtomicValue(XProcConstants._base_uri), new XdmAtomicValue(itemBaseUri.get))
        }
        item match {
          case _: TinyDocumentImpl =>
            map = map.put(new XdmAtomicValue(XProcConstants._content_type), new XdmAtomicValue("application/xml"))
          case _: TinyElementImpl =>
            map = map.put(new XdmAtomicValue(XProcConstants._content_type), new XdmAtomicValue("application/xml"))
          case _: TinyTextImpl =>
            map = map.put(new XdmAtomicValue(XProcConstants._content_type), new XdmAtomicValue("text/plain"))
          case _: StringValue =>
            map = map.put(new XdmAtomicValue(XProcConstants._content_type), new XdmAtomicValue("text/plain"))
          case _ =>
            logger.debug(s"p:document-properties knows no properties for item: ${item}")
            map = map.put(new XdmAtomicValue(XProcConstants._content_type), new XdmAtomicValue("application/octet-stream"))
        }

        map.getUnderlyingValue
      }
    }
  }
}
