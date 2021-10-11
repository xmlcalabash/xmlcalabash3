package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.util.Base64
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

class B64Decode extends DefaultXmlStep {
  private var source: Option[Any] = None
  private var smeta: Option[XProcMetadata] = None

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = Some(item)
    smeta = Some(metadata)
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val baseValue = if (smeta.isDefined) {
      smeta.get.property(XProcConstants._base_uri)
    } else {
      None
    }

    val decoded = source.get match {
      case is: InputStream =>
        Base64.getMimeDecoder.wrap(is)
      case node: XdmNode =>
        val stream = new ByteArrayInputStream(node.getStringValue.getBytes("utf-8"))
        Base64.getMimeDecoder.wrap(stream)
      case _ =>
        throw new RuntimeException(s"Don't know how to encode ${source.get}")
    }

    consumer.get.receive("result", decoded, new XProcMetadata(MediaType.OCTET_STREAM))
  }
}