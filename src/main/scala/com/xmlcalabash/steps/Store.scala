package com.xmlcalabash.steps

import java.io.{FileOutputStream, InputStream}
import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Serializer, XdmNode}

class Store extends DefaultXmlStep {
  private var source: Any = _
  private var smeta: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
      "result-uri" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*/*"),
      "result-uri" -> List("application/xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item
    smeta = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val href = if (context.baseURI.isDefined) {
      context.baseURI.get.resolve(stringBinding(XProcConstants._href))
    } else {
      new URI(stringBinding(XProcConstants._href))
    }

    if (href.getScheme != "file") {
      throw XProcException.xcCannotStore(href, context.location)
    }

    val os = new FileOutputStream(href.getPath)
    serialize(context, source, smeta, os)
    os.close()

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(smeta.baseURI)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(href.toASCIIString)
    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", source, smeta)
    consumer.get.receive("result-uri", result, new XProcMetadata(MediaType.XML))
  }
}