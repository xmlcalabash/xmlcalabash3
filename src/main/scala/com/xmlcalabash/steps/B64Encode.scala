package com.xmlcalabash.steps

import java.io.{ByteArrayOutputStream, InputStream}
import java.net.URI
import java.util.Base64
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.s9api.{QName, Serializer, XdmEmptySequence, XdmNode, XdmValue}

import scala.collection.mutable

class B64Encode extends DefaultXmlStep {
  private var source: Option[Any] = None
  private var smeta: Option[XProcMetadata] = None
  private var serialOpts = mutable.HashMap.empty[QName,String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: NameValueBinding): Unit = {
    if (variable.name == XProcConstants._serialization) {
      variable.value match {
        case _: XdmEmptySequence => ()
        case _ =>
          val opts = TypeUtils.castAsScala(variable.value).asInstanceOf[Map[Any,Any]]
          for (opt <- opts.keySet) {
            opt match {
              case name: QName =>
                serialOpts.put(name, opt.toString)
            }
          }
      }
    }
  }

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
    val baseURI = if (baseValue.isDefined) {
      Some(new URI(baseValue.get.toString))
    } else {
      None
    }

    val encoded = source.get match {
      case is: InputStream =>
        // It seems slightly odd to me that there's no streaming API for the encoder
        val stream = new ByteArrayOutputStream()
        val buf = Array.fill[Byte](4096)(0)
        var len = is.read(buf, 0, buf.length)
        while (len >= 0) {
          stream.write(buf, 0,len)
          len = is.read(buf, 0, buf.length)
        }
        Base64.getMimeEncoder.encodeToString(stream.toByteArray)
      case node: XdmNode =>
        val stream = new ByteArrayOutputStream()
        // FIXME: get serialization parameters from serialization option
        val serializer = config.processor.newSerializer(stream)

        val contentType = smeta.get.contentType
        if (!contentType.xmlContentType && !contentType.htmlContentType) {
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        }

        S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
        S9Api.configureSerializer(serializer, serialOpts.toMap)

        S9Api.serialize(config.config, node, serializer)
        Base64.getMimeEncoder.encodeToString(stream.toByteArray)
      case _ =>
        throw new RuntimeException(s"Don't know how to encode ${source.get}")
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(baseURI)
    builder.addText(encoded.replace("\r", ""))
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.TEXT))
  }
}