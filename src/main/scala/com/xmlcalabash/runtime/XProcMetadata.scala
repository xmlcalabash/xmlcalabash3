package com.xmlcalabash.runtime

import java.net.URI
import com.jafpl.messages.Metadata
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmNode, XdmValue}

import scala.collection.mutable

object XProcMetadata {
  private val _any = new XProcMetadata(MediaType.OCTET_STREAM)
  private val _xml = new XProcMetadata(MediaType.XML)
  private val _json = new XProcMetadata(MediaType.JSON)
  private val _html = new XProcMetadata(MediaType.HTML)
  private val _text = new XProcMetadata(MediaType.TEXT)
  private val _exception = new XProcMetadata(MediaType.OCTET_STREAM)
  def ANY: XProcMetadata = _any
  def XML: XProcMetadata = _xml
  def JSON: XProcMetadata = _json
  def HTML: XProcMetadata = _html
  def TEXT: XProcMetadata = _text
  def EXCEPTION: XProcMetadata = _exception
  def xml(node: XdmNode): XProcMetadata = {
    val prop = mutable.HashMap.empty[QName,XdmValue]
    prop.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
    new XProcMetadata(MediaType.XML, prop.toMap)
  }
}

class XProcMetadata(private val initialContentType: Option[MediaType],
                    private val initialProperties: Map[QName,XdmValue]) extends Metadata {
  private val _properties = mutable.HashMap.empty[QName,XdmValue]
  private var _contentType: Option[MediaType] = None
  private var _baseURI: Option[URI] = None

  for ((key,value) <- initialProperties) {
    _properties.put(key, value)
  }

  if (initialContentType.isDefined) {
    _properties.put(XProcConstants._content_type, new XdmAtomicValue(initialContentType.get.toString))
  }

  def this(contentType: Option[MediaType]) = {
    this(contentType, Map.empty[QName, XdmValue])
  }

  def this(contentType: MediaType) = {
    this(Some(contentType), Map.empty[QName,XdmValue])
  }

  def this(contentType: MediaType, baseURI: URI) = {
    this(Some(contentType), Map.empty[QName,XdmValue])
    _properties.put(XProcConstants._base_uri, new XdmAtomicValue(baseURI))
  }

  def this(contentType: MediaType, initProp: Map[QName,XdmValue]) = {
    this(Some(contentType), initProp)
  }

  def this(contentType: MediaType, metadata: XProcMetadata) = {
    this(Some(contentType), metadata.properties)
  }

  def withBaseURI(uri: URI): XProcMetadata = {
    if (baseURI.isEmpty || baseURI.get != uri) {
      val props = mutable.HashMap.empty[QName, XdmValue] ++ _properties
      props.put(XProcConstants._base_uri, new XdmAtomicValue(uri))
      new XProcMetadata(initialContentType, props.toMap)
    } else {
      this
    }
  }

  def withBaseURI(uri: Option[URI]): XProcMetadata = {
    if (uri.isDefined) {
      withBaseURI(uri.get)
    } else {
      this
    }
  }

  def properties: Map[QName,XdmValue] = _properties.toMap
  def property(name: QName): Option[XdmValue] = {
    _properties.get(name)
  }
  def property(name: String): Option[XdmValue] = {
    _properties.get(new QName("", name))
  }

  def contentType: MediaType = {
    if (_contentType.isEmpty) {
      var charset = Option.empty[String]
      if (_properties.contains(XProcConstants._serialization)) {
        val value = _properties(XProcConstants._serialization)
        val pmap = try {
          value.asInstanceOf[XdmMap]
        } catch {
          case _: ClassCastException =>
            throw XProcException.xcSerializationNotAMap(value.toString, None)
        }
        val piter = pmap.keySet.iterator()
        while (piter.hasNext) {
          val key = piter.next()
          if (key.getStringValue == "encoding") {
            charset = Some(pmap.get(key).toString)
          }
        }
      }

      if (_properties.contains(XProcConstants._content_type)) {
        val value = _properties(XProcConstants._content_type)
        if (value.size == 0) {
          _contentType = Some(MediaType.OCTET_STREAM)
        } else {
          _contentType = Some(MediaType.parse(value.itemAt(0).getStringValue, charset)) // FIXME: what about a sequence?
        }
      } else {
        _contentType = Some(MediaType.OCTET_STREAM)
      }
    }

    _contentType.get
  }

  def baseURI: Option[URI] = {
    if (_baseURI.isEmpty) {
      if (_properties.contains(XProcConstants._base_uri)) {
        val value = _properties(XProcConstants._base_uri)
        if (value.size() == 0) {
          _baseURI = None
        } else {
          _baseURI = Some(new URI(value.itemAt(0).getStringValue)) // FIXME: what about a sequence?
        }
      }
    }

    _baseURI
  }

  def castTo(contentType: MediaType): XProcMetadata = {
    castTo(contentType, List(XProcConstants._serialization))
  }

  def castTo(contentType: MediaType, except: List[QName]): XProcMetadata = {
    val prop = mutable.HashMap.empty[QName,XdmValue]
    for ((key,value) <- properties) {
      if (! except.contains(key)) {
        prop.put(key, value)
      }
    }
    new XProcMetadata(contentType, prop.toMap)
  }
}
