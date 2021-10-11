package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Base64
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.jafpl.messages.Message
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils, ValueUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable

class CastContentType() extends DefaultXmlStep {
  private var item = Option.empty[Any]
  private var metadata = Option.empty[XProcMetadata]
  private var castTo = MediaType.OCTET_STREAM
  private var parameters = Option.empty[XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    this.item = Some(item)
    this.metadata = Some(metadata)
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    variable.name match {
      case XProcConstants._content_type =>
        castTo = MediaType.parse(ValueUtils.singletonStringValue(variable.value))
      case XProcConstants._parameters =>
        parameters = Some(variable.value)
      case _ => ()
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    if (castTo.xmlContentType) {
      castToXML(context)
    } else if (castTo.jsonContentType) {
      castToJSON(context)
    } else if (castTo.htmlContentType) {
      castToHTML(context)
    } else if (castTo.textContentType) {
      castToText(context)
    } else if (castTo.classification == MediaType.OCTET_STREAM) {
      castToBinary(context)
    } else {
      throw new RuntimeException("Impossible content type cast")
    }
  }

  def castToXML(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.XML =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
      case MediaType.HTML =>
        consumer.get.receive("result", item.get, metadata.get.castTo(castTo))
      case MediaType.TEXT =>
        val text = item.get.asInstanceOf[XdmNode].getStringValue
        val bais = new ByteArrayInputStream(text.getBytes("UTF-8"))
        val baseURI = metadata.get.baseURI.getOrElse(new URI(""))
        val req = new DocumentRequest(baseURI, castTo)
        try {
          val resp = config.documentManager.parse(req, bais)
          consumer.get.receive("result", resp.value, metadata.get.castTo(castTo))
        } catch {
          case sae: SaxonApiException =>
            throw XProcException.xdNotWFXML("", sae.getMessage, location)
        }
      case MediaType.JSON =>
        // Step 1, convert the map into a JSON text string
        var expr = new XProcXPathExpression(context, "serialize($map, map {\"method\": \"json\"})")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        var vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmItem], XProcMetadata.XML, context)
        bindingsMap.put("{}map", vmsg)
        var smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

        // Step 2, convert the JSON to XML
        expr = new XProcXPathExpression(context, "json-to-xml($json)")
        bindingsMap.clear()
        vmsg = new XdmValueItemMessage(smsg.item, XProcMetadata.XML, context)
        bindingsMap.put("{}json", vmsg)
        smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

        val patched = S9Api.patchBaseURI(config.config, smsg.item.asInstanceOf[XdmNode], metadata.get.baseURI)
        consumer.get.receive("result", patched, metadata.get.castTo(castTo))
      case MediaType.OCTET_STREAM =>
        val builder = new SaxonTreeBuilder(config)

        val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
          Some(new URI(S9Api.valuesToString(metadata.get.properties(XProcConstants._base_uri))))
        } else {
          None
        }

        builder.startDocument(baseURI)

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, contentType.toString))
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._encoding, "base64"))
        builder.addStartElement(XProcConstants.c_data, amap)

        // A binary should be an input stream...
        item.get match {
          case binary: BinaryNode =>
            val is = binary.stream
            val bos = new ByteArrayOutputStream()
            var totBytes = 0L
            val pagesize = 4096
            val tmp = new Array[Byte](pagesize)
            var length = 0
            length = is.read(tmp)
            while (length >= 0) {
              bos.write(tmp, 0, length)
              totBytes += length
              length = is.read(tmp)
            }
            // The string may contain CRLF line endings, remove the CRs
            val base64str = Base64.getMimeEncoder.encodeToString(bos.toByteArray).replace("\r", "")
            builder.addText(base64str)
          case _ =>
            throw XProcException.xiUnexpectedItem(item.get.toString, location)
        }

        builder.addEndElement()
        builder.endDocument()

        val doc = builder.result
        consumer.get.receive("result", doc, metadata.get.castTo(castTo))
    }
  }

  def castToText(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.TEXT =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))

      case MediaType.XML =>
        serializeNodes(item.get.asInstanceOf[XdmNode], contentType)

      case MediaType.HTML =>
        serializeNodes(item.get.asInstanceOf[XdmNode], contentType)

      case MediaType.JSON =>
        val expr = new XProcXPathExpression(context, "serialize($map, map {\"method\": \"json\"})")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        val vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmValue], XProcMetadata.XML, context)
        bindingsMap.put("{}map", vmsg)
        val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(metadata.get.baseURI)
        builder.addText(smsg.item.toString)
        builder.endDocument()
        consumer.get.receive("result", builder.result, metadata.get.castTo(castTo))

      case MediaType.OCTET_STREAM =>
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(metadata.get.baseURI)

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, contentType.toString))
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._encoding, "base64"))

        builder.addStartElement(XProcConstants.c_data, amap)

        val stream = item.get.asInstanceOf[BinaryNode].stream
        val bos = new ByteArrayOutputStream()
        var totBytes = 0L
        val tmp = new Array[Byte](4096)
        var length = 0
        length = stream.read(tmp)
        while (length >= 0) {
          bos.write(tmp, 0, length)
          totBytes += length
          length = stream.read(tmp)
        }
        bos.close()
        stream.close()

        // The string may contain CRLF line endings, remove the CRs
        val base64str = Base64.getMimeEncoder.encodeToString(bos.toByteArray).replace("\r", "")
        builder.addText(base64str)

        builder.addEndElement()
        builder.endDocument()

        val doc = builder.result
        consumer.get.receive("result", doc, new XProcMetadata(castTo, metadata.get.properties))
    }
  }

  private def serializeNodes(item: XdmNode, contentType: MediaType): Unit = {
    val serialOpts = mutable.HashMap.empty[QName, String]
    serialOpts.put(XProcConstants._omit_xml_declaration, "true")
    // If parameters is defined, it's either a map or the empty sequence
    if (parameters.isDefined && parameters.get.isInstanceOf[XdmMap]) {
      val opts = TypeUtils.castAsScala(parameters.get).asInstanceOf[Map[Any, Any]]
      for (opt <- opts.keySet) {
        opt match {
          case name: QName =>
            serialOpts.put(name, opt.toString)
          case name: String =>
            if (!name.contains(":")) {
              serialOpts.put(new QName(name), opt.toString)
            }
        }
      }
    }

    val stream = new ByteArrayOutputStream()
    val serializer = config.processor.newSerializer(stream)

    S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
    S9Api.configureSerializer(serializer, serialOpts.toMap)

    S9Api.serialize(config.config, item, serializer)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(metadata.get.baseURI)
    builder.addText(stream.toString("UTF-8"))
    builder.endDocument()

    consumer.get.receive("result", builder.result, metadata.get.castTo(castTo))
  }

  def castToJSON(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.TEXT =>
        val expr = new XProcXPathExpression(context, "parse-json($json)")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        val vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmNode], XProcMetadata.TEXT, context)
        bindingsMap.put("{}json", vmsg)
        val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

        consumer.get.receive("result", smsg.item, metadata.get.castTo(castTo))

      case MediaType.XML =>
        val root = S9Api.documentElement(item.get.asInstanceOf[XdmNode])
        if (root.get.getNodeName == XProcConstants.fn_map
            || root.get.getNodeName == XProcConstants.fn_array) {
          var expr = new XProcXPathExpression(context, "xml-to-json($xml)")
          val bindingsMap = mutable.HashMap.empty[String, Message]
          var vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmNode], XProcMetadata.XML, context)
          bindingsMap.put("{}xml", vmsg)
          var smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

          expr = new XProcXPathExpression(context, "parse-json($json)")
          bindingsMap.clear()
          vmsg = new XdmValueItemMessage(smsg.item, XProcMetadata.TEXT, context)
          bindingsMap.put("{}json", vmsg)
          smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)

          consumer.get.receive("result", smsg.item, metadata.get.castTo(castTo))
        } else if (root.get.getNodeName == XProcConstants.c_param_set) {
          var map = new XdmMap()
          for (child <- S9Api.axis(root.get, Axis.CHILD)) {
            if (child.getNodeKind == XdmNodeKind.ELEMENT && child.getNodeName == XProcConstants.c_param) {
              val ns = Option(child.getAttributeValue(XProcConstants._namespace)).getOrElse("")
              val local = Option(child.getAttributeValue(XProcConstants._name)).get
              val name = new QName("", ns, local)
              val value = Option(child.getAttributeValue(XProcConstants._value)).getOrElse("")
              map = map.put(new XdmAtomicValue(name), new XdmAtomicValue(value))
            }
          }
          consumer.get.receive("result", map, metadata.get.castTo(castTo))
        } else {
          throw new UnsupportedOperationException("Can't cast from XML to JSON")
        }

      case MediaType.HTML =>
        throw new UnsupportedOperationException("Can't cast from HTML to JSON")

      case MediaType.JSON =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))

      case MediaType.YAML =>
        val bytes = item.get.asInstanceOf[BinaryNode].bytes
        val yamlReader = new ObjectMapper(new YAMLFactory())
        val obj = yamlReader.readValue(bytes, classOf[Object])
        val jsonWriter = new ObjectMapper()
        val json = jsonWriter.writeValueAsString(obj)

        val expr = new XProcXPathExpression(context, "parse-json($json)")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        val vmsg = new XdmValueItemMessage(new XdmAtomicValue(json), XProcMetadata.JSON, context)
        bindingsMap.put("{}json", vmsg)
        val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)
        consumer.get.receive("result", smsg.item, new XProcMetadata(castTo, smsg.metadata))

      case MediaType.OCTET_STREAM =>
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(metadata.get.baseURI)

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, contentType.toString))
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._encoding, "base64"))

        builder.addStartElement(XProcConstants.c_data, amap)

        val stream = item.get.asInstanceOf[BinaryNode].stream
        val bos = new ByteArrayOutputStream()
        var totBytes = 0L
        val pagesize = 4096
        val tmp = new Array[Byte](pagesize)
        var length = 0
        length = stream.read(tmp)
        while (length >= 0) {
          bos.write(tmp, 0, length)
          totBytes += length
          length = stream.read(tmp)
        }
        bos.close()
        stream.close()

        // The string may contain CRLF line endings, remove the CRs
        val base64str = Base64.getMimeEncoder.encodeToString(bos.toByteArray).replace("\r", "")
        builder.addText(base64str)

        builder.addEndElement()
        builder.endDocument()

        val doc = builder.result
        consumer.get.receive("result", doc, new XProcMetadata(castTo, metadata.get.properties))
    }
  }

  def castToHTML(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.TEXT =>
        val text = item.get.asInstanceOf[XdmNode].getStringValue
        val bais = new ByteArrayInputStream(text.getBytes("UTF-8"))
        val baseURI = metadata.get.baseURI.getOrElse(new URI(""))
        val req = new DocumentRequest(baseURI, castTo)
        val resp = config.documentManager.parse(req, bais)
        consumer.get.receive("result", resp.value, metadata.get.castTo(castTo))

      case MediaType.XML =>
        consumer.get.receive("result", item.get, metadata.get.castTo(castTo))

      case MediaType.HTML =>
        consumer.get.receive("result", item.get, metadata.get.castTo(castTo, List()))

      case MediaType.JSON =>
        throw new UnsupportedOperationException("Can't cast from JSON to HTML")

      case MediaType.YAML =>
        throw new UnsupportedOperationException("Can't cast from YAML to HTML")

      case MediaType.OCTET_STREAM =>
        throw new UnsupportedOperationException("Can't cast from binary to HTML")

      case _ =>
        throw new UnsupportedOperationException("Can't cast from unknown to HTML")
    }
  }

  def castToBinary(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.TEXT =>
        throw new UnsupportedOperationException("Can't cast from TEXT to binary")

      case MediaType.XML =>
        val root = S9Api.documentElement(item.get.asInstanceOf[XdmNode])
        if (root.get.getNodeName == XProcConstants.c_data) {
          val encoding = Option(root.get.getAttributeValue(XProcConstants._encoding)).getOrElse("base64")
          if (encoding != "base64") {
            throw new UnsupportedOperationException(s"Decoding $encoding data is not supported")
          }
          val cdataContentType = Option(root.get.getAttributeValue(XProcConstants._content_type))
          if (cdataContentType.isEmpty) {
            throw XProcException.xcContentTypeMissing(location)
          }
          val ctype = MediaType.parse(cdataContentType).get
          if (ctype.classification != MediaType.OCTET_STREAM) {
            throw new UnsupportedOperationException(s"Unsupported content-type on c:data, $cdataContentType")
          }

          if (castTo != ctype) {
            throw XProcException.xcDifferentContentTypes(contentType.toString, ctype.toString, location)
          }

          try {
            val bytes = Base64.getDecoder.decode(root.get.getStringValue)
            val meta = metadata.get.castTo(castTo)
            consumer.get.receive("result", bytes, meta)
          } catch {
            case iae: IllegalArgumentException =>
              throw XProcException.xcInvalidBase64(iae.getMessage, location)
          }
        } else {
          throw new UnsupportedOperationException("Can't cast from XML to binary")
        }

      case MediaType.HTML =>
        throw new UnsupportedOperationException("Can't cast from HTML to binary")

      case MediaType.JSON =>
        throw new UnsupportedOperationException("Can't cast from JSON to binary")

      case MediaType.YAML =>
        throw new UnsupportedOperationException("Can't cast from YAML to binary")

      case MediaType.OCTET_STREAM =>
        consumer.get.receive("result", item.get, metadata.get)
    }
  }
}
