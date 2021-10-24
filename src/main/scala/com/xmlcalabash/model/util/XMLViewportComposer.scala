package com.xmlcalabash.model.util

import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.{ViewportComposer, ViewportItem}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.xxml.XStaticContext
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, XProcMetadata}
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.{AttributeMap, SingletonAttributeMap}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XMLViewportComposer(config: XMLCalabash, context: XStaticContext, matchExpr: String) extends ViewportComposer {
  private val cx_viewport = new QName("cx", XProcConstants.ns_cx,"viewport")
  private val _index = new QName("index")
  private var matcher: ProcessMatch = _
  private var decomposed: XdmNode = _
  private var metadata: XProcMetadata = _
  private var items = ListBuffer.empty[XMLViewportItem]
  private var itemIndex = 0
  private var dynBindings: Map[String, Message] = _

  override def runtimeBindings(bindings: Map[String, Message]): Unit = {
    dynBindings = bindings
  }

  override def decompose(message: Message): List[ViewportItem] = {
    var source: XdmNode = null

    message match {
      case msg: XdmNodeItemMessage =>
        source = msg.item
        metadata = msg.metadata
      case msg: XdmValueItemMessage =>
        throw XProcException.xdBadViewportInput(msg.metadata.contentType, context.location)
      case _ =>
        throw XProcException.xiThisCantHappen("Viewport message without metadata?", context.location)
    }

    val bindings = mutable.HashMap.empty[String,Message] ++ context.inscopeConstantBindings
    for ((name, message) <- dynBindings) {
      bindings.put(name,message)
    }

    /*
    val expr = new XProcXPathExpression(context, patternString)
    var msg = config.expressionEvaluator.singletonValue(expr, List(), bindings.toMap, None)
    // Ok, now we have a string value
    pattern = msg.item.getUnderlyingValue.getStringValue
    */

    matcher = new ProcessMatch(config, new Decomposer(), context, bindings.toMap)
    matcher.process(source, matchExpr)
    decomposed = matcher.result
    items.toList

  }

  override def recompose(): Message = {
    matcher = new ProcessMatch(config, new Recomposer(), context)
    matcher.process(decomposed, "*") // FIXME: match only cx:viewport
    val recomposed = matcher.result
    new XdmNodeItemMessage(recomposed, metadata, context)
  }

  private class Decomposer() extends ProcessMatchingNodes {
    private def insertMarker(): Unit = {
      matcher.addStartElement(cx_viewport, SingletonAttributeMap.of(TypeUtils.attributeInfo(_index, itemIndex.toString)))
      itemIndex += 1
    }

    override def startDocument(node: XdmNode): Boolean = {
      items += new XMLViewportItem(node, metadata)
      insertMarker()
      matcher.addEndElement()
      false
    }

    override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
      items += new XMLViewportItem(node, metadata.withBaseURI(Option(node.getBaseURI)))
      insertMarker()
      false
    }

    override def endElement(node: XdmNode): Unit = {
      matcher.addEndElement()
    }

    override def endDocument(node: XdmNode): Unit = {
      matcher.endDocument()
    }

    override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
      throw XProcException.xdViewportOnAttribute(matchExpr, context.location)
    }

    override def text(node: XdmNode): Unit = {
      items += new XMLViewportItem(node, metadata.withBaseURI(Option(node.getBaseURI)))
      insertMarker()
      matcher.addEndElement()
    }

    override def comment(node: XdmNode): Unit = {
      items += new XMLViewportItem(node, metadata.withBaseURI(Option(node.getBaseURI)))
      insertMarker()
      matcher.addEndElement()
    }

    override def pi(node: XdmNode): Unit = {
      items += new XMLViewportItem(node, metadata.withBaseURI(Option(node.getBaseURI)))
      insertMarker()
      matcher.addEndElement()
    }
  }

  private class Recomposer() extends ProcessMatchingNodes {
    private def processMarker(marker: XdmNode): Unit = {
      val index = marker.getAttributeValue(_index).toInt
      matcher.addSubtree(items(index).getReplacement)
    }

    override def startDocument(node: XdmNode): Boolean = {
      matcher.startDocument(node.getBaseURI)
      true
    }

    override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
      if (node.getNodeName == cx_viewport) {
        processMarker(node)
        false
      } else {
        matcher.addStartElement(node)
        true
      }
    }

    override def endElement(node: XdmNode): Unit = {
      if (node.getNodeName != cx_viewport) {
        matcher.addEndElement()
      }
    }

    override def endDocument(node: XdmNode): Unit = {
      matcher.endDocument()
    }

    override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
      throw XProcException.xcInvalidSelection(matchExpr, "attribute", None)
    }

    override def text(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }

    override def comment(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }

    override def pi(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }
  }

  private class XMLViewportItem(item: XdmNode, meta: XProcMetadata) extends ViewportItem {
    private var replacement: XdmNode = _

    def getReplacement: XdmNode = replacement

    override def getItem: Any = {
      item
    }
    override def getMetadata: Metadata = meta
    override def putItems(items: List[Message]): Unit = {
      val lb = ListBuffer.empty[XdmNode]
      for (msg <- items) {
        msg match {
          case ni: XdmNodeItemMessage =>
            lb += ni.item
          case ai: AnyItemMessage =>
            throw XProcException.xdBadViewportResult(ai.metadata.contentType.toString, context.location)
          case _ =>
            throw XProcException.xdBadViewportResult("unknown content", context.location)
        }
      }

      if (lb.size == 1) {
        replacement = lb.head
      } else {
        val builder = new SaxonTreeBuilder(config)
        if (lb.isEmpty) {
          builder.startDocument(None)
        } else {
          builder.startDocument(lb.head.getBaseURI)
        }
        for (item <- lb) {
          builder.addSubtree(item)
        }
        builder.endDocument()
        replacement = builder.result
      }
    }
  }

}
