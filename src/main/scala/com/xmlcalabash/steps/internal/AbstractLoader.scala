package com.xmlcalabash.steps.internal

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XProcItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, DynamicContext, NameValueBinding, StaticContext, XProcExpression, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, XdmItem, XdmMap, XdmNode, XdmValue}

import scala.collection.mutable

class AbstractLoader() extends DefaultXmlStep {
  protected var content_type = Option.empty[MediaType]
  protected var _document_properties = Option.empty[String]
  protected var contextItem = Option.empty[XProcItemMessage]
  protected var msgBindings = mutable.HashMap.empty[String, XProcItemMessage]
  protected var docProps = Map.empty[QName, XdmValue]
  protected var exprContext: StaticContext = _
  protected var contentType: MediaType = _

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    val context = new StaticContext(config, None)
    item match {
      case node: XdmNode =>
        contextItem = Some(new XdmNodeItemMessage(node, meta, context))
      case item: XdmValue =>
        contextItem = Some(new XdmValueItemMessage(item, meta, context))
      case binary: BinaryNode =>
        contextItem = Some(new AnyItemMessage(binary.node, binary, meta, context))
      case item: XProcException =>
        if (item.errors.isDefined) {
          contextItem = Some(new XdmNodeItemMessage(item.errors.get, XProcMetadata.XML, context))
        }
      case _ =>
        throw new RuntimeException("Recieved unexpected item")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    if (bindings.contains(XProcConstants._content_type)) {
      content_type = Some(MediaType.parse(bindings(XProcConstants._content_type).value.getUnderlyingValue.getStringValue))
    }

    // Fake the statics
    for ((name,message) <- exprContext.statics) {
      val msg = message.asInstanceOf[XdmValueItemMessage]
      val qname = ValueParser.parseClarkName(name)
      receiveBinding(new NameValueBinding(qname, msg))
    }

    for ((name, binding) <- bindings) {
      binding.value match {
        case node: XdmNode =>
          val message = new XdmNodeItemMessage(node, XProcMetadata.XML, context)
          msgBindings.put(name.getClarkName, message)
        case item: XdmValue =>
          val message = new XdmValueItemMessage(item, XProcMetadata.XML, context)
          msgBindings.put(name.getClarkName, message)
      }
    }

    if (_document_properties.isDefined) {
      val expr = new XProcXPathExpression(exprContext, _document_properties.get)
      val result = xpathValue(expr)
      docProps = result match {
        case map: XdmMap =>
          ValueParser.parseDocumentProperties(map, exprContext, location)
        case _ =>
          throw XProcException.xsBadTypeValue("document-properties", "map", location)
      }
    }
  }

  protected def xpathValue(expr: XProcExpression): XdmValue = {
    val dynContext = new DynamicContext()
    val eval = config.expressionEvaluator.newInstance()
    val msg = eval.withContext(dynContext) { eval.singletonValue(expr, contextItem.toList, msgBindings.toMap, None) }
    msg.item
  }
}
