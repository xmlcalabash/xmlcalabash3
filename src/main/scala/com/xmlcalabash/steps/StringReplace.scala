package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{S9Api, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmValue}

import scala.jdk.CollectionConverters.ListHasAsScala

class StringReplace() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _replace = new QName("", "replace")
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var replace: String = _
  private var replContext: StaticContext = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/xml", "text/html")))

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result"->List("application/xml", "text/html", "text/plain")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    pattern = stringBinding(XProcConstants._match)
    replace = stringBinding(_replace)
    replContext = bindings(_replace).context

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, source_metadata))
    }

  private def computeReplacement(context: XdmNode): XdmValue = {
    val compiler = config.processor.newXPathCompiler()
    if (replContext.baseURI.isDefined) {
      compiler.setBaseURI(replContext.baseURI.get)
    }
    for ((pfx, uri) <- replContext.nsBindings) {
      compiler.declareNamespace(pfx, uri)
    }
    val expr = compiler.compile(replace)
    val selector = expr.load()
    selector.setContextItem(context)
    selector.evaluate()
  }

  def replaceNode(context: XdmNode): Unit = {
    val value = computeReplacement(context)
    for (pos <- 0 until value.size()) {
      val item = value.itemAt(pos)
      item match {
        case node: XdmNode =>
          matcher.addSubtree(node)
        case _ =>
          matcher.addText(item.getStringValue)
      }
    }
  }

  override def startDocument(node: XdmNode): Boolean = {
    replaceNode(node)
    false
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    replaceNode(node)
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop, replaced
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop, replaced
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    for (attr <- nonMatchingAttributes.asList().asScala) {
      amap = amap.put(attr)
    }
    for (attr <- matchingAttributes.asList().asScala) {
      // This is kind of ugly; I need the XdmNode for the attribute
      var attrNode: XdmNode = null
      for (anode <- S9Api.axis(node, Axis.ATTRIBUTE)) {
        val aname = TypeUtils.fqName(anode.getNodeName)
        if (aname == attr.getNodeName) {
          attrNode = anode
        }
      }
      val replacement = computeReplacement(attrNode).getUnderlyingValue.getStringValue
      amap = amap.put(new AttributeInfo(attr.getNodeName, BuiltInAtomicType.UNTYPED_ATOMIC, replacement, attr.getLocation, ReceiverOption.NONE))
    }
    Some(amap)

  }

  override def text(node: XdmNode): Unit = {
    replaceNode(node)
  }

  override def comment(node: XdmNode): Unit = {
    replaceNode(node)
  }

  override def pi(node: XdmNode): Unit = {
    replaceNode(node)
  }
}
