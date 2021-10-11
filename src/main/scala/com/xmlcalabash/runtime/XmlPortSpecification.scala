package com.xmlcalabash.runtime

import com.jafpl.steps.{PortCardinality, PortSpecification}
import com.xmlcalabash.exceptions.XProcException

import scala.collection.immutable

/** Useful default port binding specifications.
  *
  */
object XmlPortSpecification {
  /** Allow anything on any ports. */
  val ANY: XmlPortSpecification = new XmlPortSpecification(
    Map(PortSpecification.WILDCARD->PortCardinality.ZERO_OR_MORE),
    Map(PortSpecification.WILDCARD -> List("application/octet-stream")))

  /** Allow XML on any ports. */
  val ANYXML: XmlPortSpecification = new XmlPortSpecification(
    Map(PortSpecification.WILDCARD->PortCardinality.ZERO_OR_MORE),
    Map(PortSpecification.WILDCARD -> List("application/xml")))

  /** Allow no ports. */
  val NONE: XmlPortSpecification = new XmlPortSpecification(Map(), Map())

  /** Allow a single document of any sort on the `source` port. */
  val ANYSOURCE: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/octet-stream")))

  /** Allow a single document of any sort on the `result` port. */
  val ANYRESULT: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result"->List("application/octet-stream")))

  /** Allow a single HTML document on the `result` port. */
  val HTMLRESULT: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result" -> List("application/xhtml+xml")))

  /** Allow a single TEXT document on the `source` port. */
  val TEXTSOURCE: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("text")))

  /** Allow a single JSON document on the `source` port. */
  val JSONSOURCE: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("json")))

  /** Allow a sequence of TEXT documents on the `source` port. */
  val TEXTSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.ZERO_OR_MORE),
    Map("source"->List("text")))

  /** Allow a single TEXT document on the `result` port. */
  val TEXTRESULT: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result" -> List("text")))

  /** Allow a single XML document on the `source` port. */
  val XMLSOURCE: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/xml")))

  /** Allow a single XML document on the `result` port. */
  val XMLRESULT: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result" -> List("application/xml")))

  /** Allow a single XML or HTML document on the `source` port. */
  val MARKUPSOURCE: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/xml", "text/html")))

  /** Allow a single JSON document on the `result` port. */
  val JSONRESULT: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result" -> List("json")))

  /** Allow a sequence of zero or more documents of any sort on the `source` port. */
  val ANYSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.ZERO_OR_MORE),
    Map("source"->List("application/octet-stream")))

  /** Allow a sequence of zero or more documents of any sort on the `result` port. */
  val ANYRESULTSEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.ZERO_OR_MORE),
    Map("result"->List("application/octet-stream")))

  /** Allow a sequence of zero or more JSON documents on the `source` port. */
  val JSONSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.ZERO_OR_MORE),
    Map("source"->List("json")))

  /** Allow a sequence of zero or more JSON documents on the `result` port. */
  val JSONRESULTSEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.ZERO_OR_MORE),
    Map("result"->List("json")))

  /** Allow a sequence of zero or more XML documents on the `source` port. */
  val XMLSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.ZERO_OR_MORE),
    Map("source"->List("application/xml")))

  /** Allow a sequence of zero or more XML documents on the `result` port. */
  val XMLRESULTSEQ: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.ZERO_OR_MORE),
    Map("result"->List("application/xml")))
}

class XmlPortSpecification(spec: immutable.Map[String,PortCardinality],
                           accept: immutable.Map[String, List[String]]) extends PortSpecification(spec) {
  for (port <- accept.keySet) {
    if (!spec.contains(port)) {
      throw XProcException.xiNoSuchPortOnAccept(port)
    }
  }

  def accepts(port: String, contentType: String): Boolean = {
    if (spec.contains(port)) {
      if (accept.contains(port)) {
        val list = accept(port)
        if (list.contains("application/octet-stream")) {
          true
        } else {
          // FIXME: Handle the subtle cases like application/xml+rdf => application/xml
          list.contains(contentType)
        }
      } else {
        true
      }
    } else {
      false
    }
  }

  override def toString: String = {
    s"XmlPortSpecification $spec"
  }

}
