package com.xmlcalabash.steps

import java.net.URI
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MinimalStaticContext
import com.xmlcalabash.util.TypeUtils.castAsXml
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmNode, XdmValue}
import org.apache.xerces.util.URI.MalformedURIException

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

  override def run(context: MinimalStaticContext): Unit = {
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
      qname match {
        case XProcConstants._content_type =>
          throw XProcException.xcContentTypeNotAllowed(context.location)
        case XProcConstants._serialization =>
          // Make sure the serialization map is map(xs:QName,item()*)
          value match {
            case map: XdmMap =>
              try {
                val smap = ValueParser.parseDocumentProperties(map, context, location)
                var xmap = new XdmMap()
                for ((key,mval) <- smap) {
                  val obj = castAsXml(mval)
                  xmap = xmap.put(new XdmAtomicValue(key), obj)
                }
                newprops.put(qname, map)
             } catch {
                case ex: XProcException =>
                  if (ex.code == XProcException.err_xd0036) {
                    throw XProcException.xdBadMapKey(ex.details.head.asInstanceOf[String], location)
                  }
                  throw ex
                case ex: Throwable =>
                  throw ex
              }
            case _ =>
              throw XProcException.xdInvalidSerialization("Value is not a map", location)
          }
        case _ =>
          newprops.put(qname, value)
      }
    }

    var result = source
    // The base URI is special; make sure it's not some bogus string
    if (newprops.contains(XProcConstants._base_uri)) {
      val ustr = newprops(XProcConstants._base_uri).toString
      try {
        val uri = new URI(ustr)
        if (!uri.isAbsolute) {
          throw XProcException.xdInvalidURI(ustr, location)
        }
        newprops.put(XProcConstants._base_uri, new XdmAtomicValue(uri))
      } catch {
        case _: MalformedURIException =>
          throw XProcException.xdInvalidURI(ustr, location)
        case ex: Exception =>
          throw ex
      }
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
    consumer.receive("result", result, newmeta)
  }
}
