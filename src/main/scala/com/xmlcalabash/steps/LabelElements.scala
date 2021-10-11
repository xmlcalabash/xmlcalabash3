package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, FingerprintedQName, NamespaceMap}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode, XdmValue}

import java.net.URI
import scala.jdk.CollectionConverters.IterableHasAsScala

class LabelElements() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _attribute = new QName("attribute")
  private val _label = new QName("label")
  private val _replace = new QName("replace")
  private val p_index = new QName("p", XProcConstants.ns_p, "index")

  private var context: StaticContext = _
  private var attribute: QName = _
  private var label: String = _
  private var labelNamespaceBindings = Map.empty[String, String]
  private var pattern: String = _
  private var replace = true
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var p_count = 1L

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == _label) {
      labelNamespaceBindings = variable.context.nsBindings
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    attribute = qnameBinding(_attribute).get
    label = stringBinding(_label)
    pattern = stringBinding(XProcConstants._match)
    replace = booleanBinding(_replace).getOrElse(false)
    this.context = context

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    var nsmap: NamespaceMap = node.getUnderlyingNode.getAllNamespaces
    var amap: AttributeMap = EmptyAttributeMap.getInstance()

    val prefix = prefixFor(nsmap, attribute.getPrefix, attribute.getNamespaceURI)
    val aname = new FingerprintedQName(prefix, attribute.getNamespaceURI, attribute.getLocalName)

    if (Option(nsmap.getURI(prefix)).isEmpty) {
      nsmap = nsmap.put(prefix, attribute.getNamespaceURI)
    }

    var found = false
    for (ainfo <- attributes.asScala) {
      if (aname == ainfo.getNodeName) {
        found = true
        if (replace) {
          amap = amap.put(new AttributeInfo(aname, BuiltInAtomicType.UNTYPED_ATOMIC, computedLabel(node), ainfo.getLocation, ReceiverOption.NONE))
        } else {
          amap = amap.put(ainfo)
        }
      } else {
        amap = amap.put(ainfo)
      }
    }

    if (!found) {
      amap = amap.put(new AttributeInfo(aname, BuiltInAtomicType.UNTYPED_ATOMIC, computedLabel(node), null, ReceiverOption.NONE))
    }
    p_count += 1

    matcher.addStartElement(node.getNodeName, amap, nsmap)
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "processing-instruction", location)
  }

  private def computedLabel(node: XdmNode): String = {
    val xcomp = config.processor.newXPathCompiler()
    if (location.isDefined && location.get.uri.isDefined) {
      xcomp.setBaseURI(new URI(location.get.uri.get))
    }

    for ((prefix,uri) <- labelNamespaceBindings) {
      xcomp.declareNamespace(prefix, uri)
    }

    xcomp.declareVariable(p_index)
    val xexec = xcomp.compile(label)
    val selector = xexec.load()
    selector.setVariable(p_index, new XdmAtomicValue(p_count))
    selector.setContextItem(node)
    val values = selector.iterator()
    val item = values.next()
    item.getStringValue
  }

}
