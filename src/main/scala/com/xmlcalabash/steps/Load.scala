package com.xmlcalabash.steps

import java.net.URI
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmMap, XdmValue}

import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

class Load() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANY

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val href = uriBinding(XProcConstants._href);

    // FIXME: the key type conversions here should occur centrally based on map type.

    val params = mutable.HashMap.empty[QName, XdmValue]
    if (definedBinding(XProcConstants._parameters)) {
      val _params = bindings(XProcConstants._parameters).value
      _params match {
        case map: XdmMap =>
          for (key <- map.keySet.asScala) {
            val value = map.get(key)
            val qname = key.getPrimitiveTypeName match {
              case XProcConstants.xs_string => new QName("", "", key.getStringValue)
              case XProcConstants.xs_QName => key.getQNameValue
              case _ => throw new IllegalArgumentException("Unexpected key type: " + key.getTypeName)
            }
            params.put(qname, value)
          }
        case _ => throw new IllegalArgumentException("Map was expected")
      }
    }

    val docprops = mutable.HashMap.empty[QName, XdmValue]
    if (definedBinding(XProcConstants._document_properties)) {
      val _props = bindings(XProcConstants._document_properties).value
      _props match {
        case map: XdmMap =>
          for (key <- map.keySet.asScala) {
            val value = map.get(key)
            val qname = key.getPrimitiveTypeName match {
              case XProcConstants.xs_string => new QName("", "", key.getStringValue)
              case XProcConstants.xs_QName => key.getQNameValue
              case _ => throw new IllegalArgumentException("Unexpected key type: " + key.getTypeName)
            }
            docprops.put(qname, value)
          }
        case _ => throw new IllegalArgumentException("Map was expected")
      }
    }

    val declContentType = if (definedBinding(XProcConstants._content_type)) {
      Some(MediaType.parse(stringBinding(XProcConstants._content_type)))
    } else {
      None
    }

    val dtdValidate = if (params.contains(XProcConstants._dtd_validate)) {
      if (params(XProcConstants._dtd_validate).size > 1) {
        throw new IllegalArgumentException("dtd validate parameter is not a singleton")
      } else {
        params(XProcConstants._dtd_validate).itemAt(0).getStringValue == "true"
      }
    } else {
      false
    }

    val request = new DocumentRequest(href, declContentType, location, params.toMap)
    request.docprops = docprops.toMap

    val result = config.documentManager.parse(request)

    // This feels like it's in the wrong place; like it should be centralized somehow...
    if (result.shadow.isDefined) {
      val node = new BinaryNode(config, result.shadow.get)
      consumer.get.receive("result", node, new XProcMetadata(result.contentType, result.props))
    } else {
      consumer.get.receive("result", result.value, new XProcMetadata(result.contentType, result.props))
    }
  }
}
