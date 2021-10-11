package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmValue}

import scala.collection.mutable

class NamespaceDelete() extends DefaultXmlStep {
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var namespaces: mutable.HashSet[String] = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    namespaces = mutable.HashSet.empty[String]
    val prefixes = bindings(XProcConstants._prefixes).value.getUnderlyingValue.getStringValue.split("\\s+")
    for (prefix <- prefixes) {
      val uri = context.nsBindings.get(prefix)
      if (uri.isDefined) {
        namespaces.add(uri.get)
      } else {
        throw XProcException.xcPrefixNotInScope(prefix, location)
      }
    }

    val doc = S9Api.removeNamespaces(config.config, source, namespaces.toSet, false)
    consumer.get.receive("result", doc, metadata)
  }
}
