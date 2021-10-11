package com.xmlcalabash.steps

import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XProcItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class Filter extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.MARKUPSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  private var source: XdmNode = _
  private var meta: XProcMetadata = _

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        source = node
        meta = metadata
      case _ =>
        throw XProcException.xiThisCantHappen("Non-markup document sent to p:filter", location)
    }
  }

  override def run(context: StaticContext): Unit = {
    val select = stringBinding(XProcConstants._select)

    val expr = new XProcXPathExpression(context, select)
    val contextItem = new XdmValueItemMessage(source, meta, context)
    val namebindings = mutable.HashMap.empty[String, Message]

    for ((name, bind) <- bindings) {
      namebindings.put(name.getClarkName, new XdmValueItemMessage(bind.value, bind.meta, bind.context))
    }

    val eval = config.expressionEvaluator.newInstance()
    val selected = eval.value(expr, List(contextItem), namebindings.toMap, None)

    val iter = selected.item.iterator()
    while (iter.hasNext) {
      val builder = new SaxonTreeBuilder(config)
      builder.startDocument(meta.baseURI)
      iter.next() match {
        case node: XdmNode =>
          builder.addSubtree(node)
        case item =>
          builder.addText(item.getStringValue)
      }
      builder.endDocument()
      consumer.get.receive("result", builder.result, new XProcMetadata(meta.contentType))
    }
  }
}
