package com.xmlcalabash.steps

import java.net.URI
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode, XdmValue}

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class SetProperties() extends DefaultXmlStep {
  private val _merge = new QName("merge")

  private var source: Any = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item
      this.metadata = metadata
    } else {
      throw new IllegalArgumentException(s"p:set-properties received input on port: $port")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val properties = mapBinding(XProcConstants._properties)

    val newprops = mutable.HashMap.empty[QName,XdmValue]

    if (booleanBinding(_merge).getOrElse(false)) {
      for ((name, value) <- metadata.properties) {
        newprops.put(name, value)
      }
    }

    for ((name,value) <- properties.asMap.asScala) {
      val qname = name.getQNameValue
      if (qname == XProcConstants._content_type) {
        throw XProcException.xcContentTypeNotAllowed(context.location)
      }
      newprops.put(qname, value)
    }

    var result = source
    // The base URI is special; make sure it's not some bogus string
    if (newprops.contains(XProcConstants._base_uri)) {
      val uri = if (context.baseURI.isDefined) {
        resolveURI(context.baseURI.get, newprops(XProcConstants._base_uri).toString)
      } else {
        new URI(newprops(XProcConstants._base_uri).toString)
      }
      newprops.put(XProcConstants._base_uri, new XdmAtomicValue(uri))
    } else {
      if (metadata.properties.contains(XProcConstants._base_uri)) {
        // The base URI property has been removed...
        source match {
          case node: XdmNode =>
            val rebuild = new SaxonTreeBuilder(this.config)
            rebuild.startDocument(None)
            rebuild.addSubtree(node)
            rebuild.endDocument()
            result = rebuild.result
          case _ =>
            logger.debug(s"Cannot remove base URI from non-node: ${source}")
        }
      }
    }

    val newmeta = new XProcMetadata(metadata.contentType, newprops.toMap)
    consumer.get.receive("result", result, newmeta)
  }
}
