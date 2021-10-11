package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcVtExpression}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala

class TvtExpander(config: XMLCalabashConfig,
                  contextItem: Option[XProcItemMessage],
                  exprContext: StaticContext,
                  msgBindings: Map[String, XProcItemMessage],
                  location: Option[Location]) {
  private val excludeURIs = mutable.HashSet.empty[String]
  private var expandText = false
  private val fq_inline_expand_text = TypeUtils.fqName(XProcConstants._inline_expand_text)
  private val fq_p_inline_expand_text = TypeUtils.fqName(XProcConstants.p_inline_expand_text)

  def expand(node: XdmNode, initallyExpand: Boolean, exclude: Set[String]): XdmNode = {
    expandText = initallyExpand
    excludeURIs.clear()
    excludeURIs ++= exclude

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    // FIXME: trim whitespace
    expandTVT(node, builder, expandText)
    builder.endDocument()
    builder.result
  }

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        var nsmap = NamespaceMap.emptyMap()
        val iter = node.getUnderlyingNode.getAllNamespaces.iterator()
        while (iter.hasNext) {
          val ns = iter.next()
          if (!excludeURIs.contains(ns.getURI)) {
            val prefix = Option(ns.getPrefix).getOrElse("")
            nsmap = nsmap.put(prefix, ns.getURI)
          }
        }

        var newExpand = expandText

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        for (attr <- node.getUnderlyingNode.attributes().asList().asScala) {
          var discardAttribute = false
          if (attr.getNodeName == fq_p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(exprContext.location)
            }
            discardAttribute = true
            newExpand = attr.getValue == "true"
          }
          if (attr.getNodeName == fq_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getValue == "true"
            }
          }
          if (!discardAttribute) {
            if (expandText) {
              amap = amap.put(new AttributeInfo(attr.getNodeName, BuiltInAtomicType.UNTYPED_ATOMIC, expandString(attr.getValue), null, ReceiverOption.NONE))
            } else {
              amap = amap.put(attr)
            }
          }
        }

        builder.addStartElement(node.getNodeName, amap, nsmap)
        val citer = node.axisIterator(Axis.CHILD)
        while (citer.hasNext) {
          val child = citer.next()
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          expandNodes(str, builder)
        } else {
          builder.addText(str.replace("}}", "}"))
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String): String = {
    val expr = new XProcVtExpression(exprContext, text)
    var s = ""
    var string = ""
    val evaluator = config.expressionEvaluator.newInstance()
    val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      string = string + s + next.getStringValue
      s = " "
    }
    string
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val expr = new XProcVtExpression(exprContext, text)

    val evaluator = config.expressionEvaluator.newInstance()
    val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      next match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(next.getStringValue)
      }
    }
  }

}
