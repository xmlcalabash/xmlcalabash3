package com.xmlcalabash.steps.internal

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{BinaryNode, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.{XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable.ListBuffer

class XPathSelector(config: XMLCalabash,
                    items: List[Tuple2[Any, XProcMetadata]],
                    select: String,
                    context: MinimalStaticContext,
                    bindings: Map[String,Message]) {
  private val results = ListBuffer.empty[Any]

  def select(): List[Any] = {
    if (items.isEmpty) {
      makeSelection(List())
    } else {
      for (item <- items) {
        val node = item._1
        val metadata = item._2
        val msg = node match {
          case value: XdmNode =>
            new XdmNodeItemMessage(value, metadata, context)
          case value: XdmValue =>
            new XdmValueItemMessage(value, metadata, context)
          case value: BinaryNode =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(metadata.baseURI)
            tree.endDocument()
            new AnyItemMessage(tree.result, value, metadata, context)
          case _ =>
            throw XProcException.xiThisCantHappen(s"Unexpected node type ${node}", context.location)
        }
        makeSelection(List(msg))
      }
    }

    results.toList
  }

  private def makeSelection(contextItem: List[Message]): Unit = {
    val expr = new XProcXPathExpression(context, select, None, None, None)
    val exprEval = config.expressionEvaluator.newInstance()
    val result = exprEval.value(expr, contextItem, bindings, None)
    val iter = result.item.iterator()
    var count = 0
    while (iter.hasNext) {
      val item = iter.next()
      count += 1

      item match {
        case node: XdmNode =>
          if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
            throw XProcException.xdInvalidSelection(select, "attribute", context.location)
          }
          val tree = new SaxonTreeBuilder(config)
          tree.startDocument(node.getBaseURI)
          tree.addSubtree(node)
          tree.endDocument()
          results += tree.result
        case _ =>
          results += item
      }
    }
  }
}
