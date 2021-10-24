package com.xmlcalabash.test

import com.jafpl.graph.Location
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.XMLStaticContext
import net.sf.saxon.s9api.QName
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

object VoidLocation extends Location {
  override def uri: Option[String] = None
  override def line: Option[Long] = None
  override def column: Option[Long] = None
}

class XStaticContextSpec extends AnyFlatSpec {
  private val nsmap = mutable.HashMap.empty[String,String]

  nsmap.put("p", XProcConstants.ns_p)
  nsmap.put("cx", XProcConstants.ns_cx)

  private val staticContext = new XMLStaticContext(new QName("", "irrelevant"), VoidLocation, nsmap.toMap)

  "p:foo " should "be a valid QName" in {
    val qname = staticContext.parseQName("p:foo")
    assert(qname.getPrefix == "p")
    assert(qname.getNamespaceURI == XProcConstants.ns_p)
    assert(qname.getLocalName == "foo")
  }

  "foo " should "be a valid QName" in {
    val qname = staticContext.parseQName("foo")
    assert(qname.getPrefix == "")
    assert(qname.getNamespaceURI == "")
    assert(qname.getLocalName == "foo")
  }

  "Q{http://example.com/}foo " should "be a valid QName" in {
    val qname = staticContext.parseQName("Q{http://example.com/}foo")
    assert(qname.getPrefix == "")
    assert(qname.getNamespaceURI == "http://example.com/")
    assert(qname.getLocalName == "foo")
  }

  ":foo " should " throw an exception" in {
    try {
      staticContext.parseQName(":foo")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }

  "xyz:foo " should " throw an exception" in {
    try {
      staticContext.parseQName("xyz:foo")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }

  "Q{foo " should " throw an exception" in {
    try {
      staticContext.parseQName("Q{foo")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }

  "3 " should " throw an exception" in {
    try {
      staticContext.parseQName("3")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }

  "p:3 " should " throw an exception" in {
    try {
      staticContext.parseQName("p:3")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }

  "p:foo:bar " should " throw an exception" in {
    try {
      staticContext.parseQName("p:foo:bar")
      fail()
    } catch {
      case _: Exception =>
        ()
    }
  }


}
