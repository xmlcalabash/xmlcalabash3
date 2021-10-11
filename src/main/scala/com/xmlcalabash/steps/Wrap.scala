package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.{Axis, QName, XdmItem, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable

class Wrap() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _group_adjacent = new QName("", "group-adjacent")
  private val _wrapper = new QName("", "wrapper")

  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var groupAdjacent = Option.empty[String]
  private var groupAdjacentContext = Option.empty[StaticContext]
  private var wrapper: QName = _
  private val inGroup = mutable.Stack[Boolean]()
  private var staticContext: StaticContext = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/xml", "text/html")))

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    staticContext = context

    pattern = stringBinding(XProcConstants._match)
    wrapper = qnameBinding(_wrapper).get
    groupAdjacent = optionalStringBinding(_group_adjacent)
    if (groupAdjacent.isDefined) {
      groupAdjacentContext = Some(bindings(_group_adjacent).context)
    }

    inGroup.push(false)

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, source_metadata)
    }

  override def startDocument(node: XdmNode): Boolean = {
    matcher.startDocument(node.getBaseURI)
    matcher.addStartElement(wrapper)
    matcher.addSubtree(node)
    matcher.addEndElement()
    matcher.endDocument()
    false
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    if (!inGroup.head) {
      matcher.addStartElement(wrapper)
    }

    inGroup.pop()
    inGroup.push(groupAdjacent.isDefined && nextMatches(node))

    matcher.addStartElement(node, attributes)

    inGroup.push(false) // endElement will pop this, its value doesn't matter!
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
    inGroup.pop()
    if (!inGroup.head) {
      matcher.addEndElement()
    }
  }

  override def attributes(node: XdmNode, matching: AttributeMap, nonMatching: AttributeMap): Option[AttributeMap] = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    simpleNode(node)
  }

  override def comment(node: XdmNode): Unit = {
    simpleNode(node)
  }

  override def pi(node: XdmNode): Unit = {
    simpleNode(node)
  }

  private def simpleNode(node: XdmNode): Unit = {
    if (!inGroup.head) {
      matcher.addStartElement(wrapper)
    }

    matcher.addSubtree(node)

    inGroup.pop()
    if (groupAdjacent.isDefined && nextMatches(node)) {
      inGroup.push(true)
    } else {
      matcher.addEndElement()
      inGroup.push(false)
    }
  }

  private def nextMatches(node: XdmNode): Boolean = {
    val nodeValue = computeGroup(node)
    if (nodeValue.isEmpty) {
      return false
    }

    val iter = node.axisIterator(Axis.FOLLOWING_SIBLING)
    while (iter.hasNext) {
      val chk = iter.next
      val skipable = chk.getNodeKind match {
        case XdmNodeKind.TEXT =>
          chk.getStringValue.trim == ""
        case XdmNodeKind.COMMENT =>
          true
        case XdmNodeKind.PROCESSING_INSTRUCTION =>
          true
        case _ =>
          false
      }

      if (matcher.matches(chk)) {
        val nextItem = computeGroup(chk)
        return S9Api.xpathEqual(config, nodeValue.get, nextItem.get)
      }

      if (!skipable) {
        return false
      }
    }

    false
  }

  private def computeGroup(node: XdmNode): Option[XdmItem] = {
    val xcomp = config.processor.newXPathCompiler()
    if (staticContext.baseURI.isDefined) {
      xcomp.setBaseURI(staticContext.baseURI.get)
    }
    for ((pfx,uri) <- groupAdjacentContext.get.nsBindings) {
      xcomp.declareNamespace(pfx, uri)
    }

    val xexec = xcomp.compile(groupAdjacent.get)
    val selector = xexec.load()
    selector.setContextItem(node)

    val values = selector.iterator()
    if (values.hasNext) {
      return Some(values.next())
    }
    None
  }
}







































