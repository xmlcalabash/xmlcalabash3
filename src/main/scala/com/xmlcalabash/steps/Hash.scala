package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{HashUtils, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap}
import net.sf.saxon.s9api.{QName, XdmNode}
import net.sf.saxon.value.QNameValue

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IterableHasAsScala, SeqHasAsJava}

class Hash() extends DefaultXmlStep  with ProcessMatchingNodes {
  private val _value = new QName("", "value")
  private val _algorithm = new QName("", "algorithm")
  private val _version = new QName("", "version")
  private val _crc = new QName("", "crc")
  private val _md = new QName("", "md")
  private val _sha = new QName("", "sha")
  private val cx_hmac = new QName("cx", XProcConstants.ns_cx, "hmac")

  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var hash: String = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    this.metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val value = stringBinding(_value).getBytes("UTF-8")
    val algorithm = qnameBinding(_algorithm).get

    val version = if (definedBinding(_version)) {
      stringBinding(_version)
    } else {
      algorithm match {
        case `_crc` => "32"
        case `_md`  =>  "5"
        case `_sha` =>  "1"
        case _      =>   ""
      }
    }

    algorithm match {
      case `_crc` =>
        hash = HashUtils.crc(value, version, location)
      case `_md` =>
        hash = HashUtils.md(value, version, location)
      case `_sha` =>
        hash = HashUtils.sha(value, version, location)
      case `cx_hmac` =>
        if (definedBinding(XProcConstants._parameters)) {
          val key = bindings(XProcConstants._parameters)
          val map = TypeUtils.castAsScala(key).asInstanceOf[Map[Any,Any]]
          if (map.contains("accessKey")) {
            hash = HashUtils.hmac(value, version, map("accessKey").toString, location)
          } else {
            throw XProcException.xcMissingHmacKey(location)
          }
        } else {
          throw XProcException.xcMissingHmacKey(location)
        }
      case _ =>
        throw XProcException.xcBadHashAlgorithm(algorithm.toString, location)
    }

    pattern = stringBinding(XProcConstants._match)
    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    if (documentIsText(result)) {
      metadata = convertMetadataToText(metadata)
    }

    consumer.get.receive("result", result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    matcher.addText(hash)
    false
  }

  override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
    matcher.addText(hash)
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
    val alist = ListBuffer.empty[AttributeInfo]
    alist ++= nonMatchingAttributes.asScala
    for (attr <- matchingAttributes.asScala) {
      alist += new AttributeInfo(attr.getNodeName, BuiltInAtomicType.ANY_ATOMIC, hash, attr.getLocation, ReceiverOption.NONE)
    }
    Some(AttributeMap.fromList(alist.toList.asJava))
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(hash)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addText(hash)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addText(hash)
  }
}
