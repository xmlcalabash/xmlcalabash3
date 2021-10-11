package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.GZIPOutputStream
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

import scala.collection.mutable

class Compress extends DefaultXmlStep {
  private val _gzip = new QName("", "gzip")

  private var source: Any = _
  private var metadata: XProcMetadata = _

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item
    metadata = meta
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == XProcConstants._parameters && variable.value.size() > 0) {
      parameters = ValueParser.parseParameters(variable.value, variable.context)
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    format = qnameBinding(XProcConstants._format)

    if (format.get != _gzip) {
      throw XProcException.xcUnknownCompressionFormat(format.get, location)
    }

    val baos = new ByteArrayOutputStream()
    serialize(context, source, metadata, baos)
    baos.close()
    val bais = new ByteArrayInputStream(baos.toByteArray)

    val gzos = new ByteArrayOutputStream()
    val gzip = new GZIPOutputStream(gzos)

    val bytes = new Array[Byte](8192)
    var count = bais.read(bytes)
    while (count >= 0) {
      gzip.write(bytes, 0, count)
      count = bais.read(bytes)
    }
    gzip.close()
    gzos.close()

    val binary = new BinaryNode(config, gzos.toByteArray)

    val mtype = new MediaType("application", "gzip")
    val props = mutable.HashMap.empty[QName, XdmValue]
    for ((key,value) <- metadata.properties) {
      if (key != XProcConstants._serialization) {
        props.put(key,value)
      }
    }

    val meta = new XProcMetadata(mtype, props.toMap)
    consumer.get.receive("result", binary, meta)
  }
}