package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, ZipException}
import com.xmlcalabash.config.{DocumentRequest, DocumentResponse}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

import scala.collection.mutable

class Uncompress extends DefaultXmlStep {
  private val _gzip = new QName("", "gzip")
  private val applicationGzip = new MediaType("application", "gzip")

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
    if (format.isEmpty) {
      if (metadata.contentType.matches(applicationGzip)) {
        format = Some(_gzip)
      } else {
        if (metadata.contentType.classification == MediaType.OCTET_STREAM) {
          format = Some(_gzip)
        } else {
          throw XProcException.xcUnknownCompressionFormat(location)
        }
      }
    }

    if (format.get != _gzip) {
      throw XProcException.xcUnknownCompressionFormat(format.get, location)
    }

    val outputContentType = MediaType.parse(stringBinding(XProcConstants._content_type))

    val baos = new ByteArrayOutputStream()
    serialize(context, source, metadata, baos)
    baos.close()
    val bais = new ByteArrayInputStream(baos.toByteArray)

    val gos = new ByteArrayOutputStream()
    try {
      val gunzip = new GZIPInputStream(bais)

      val bytes = new Array[Byte](8192)
      var count = gunzip.read(bytes)
      while (count >= 0) {
        gos.write(bytes, 0, count)
        count = gunzip.read(bytes)
      }
      gunzip.close()
      gos.close()
    } catch {
      case _: ZipException =>
        throw XProcException.xcUncompressionError(location)
      case ex: Exception =>
        throw ex
    }

    val gis = new ByteArrayInputStream(gos.toByteArray)

    var result: DocumentResponse = null
    try {
      val request = new DocumentRequest(context.baseURI.get, Some(outputContentType), location)
      result = config.documentManager.parse(request, gis)
    } catch {
      case _: XProcException =>
        throw XProcException.xcInvalidResultDataFormat(location)
      case ex: Exception =>
        throw ex
    }

    val props = mutable.HashMap.empty[QName, XdmValue]
    for ((key,value) <- metadata.properties) {
      // content-length after we've uncompressed is misleading
      if (key != XProcConstants._content_length) {
        props.put(key,value)
      }
    }

    val meta = new XProcMetadata(result.contentType, props.toMap)
    consumer.get.receive("result", result.value, meta)
  }
}