package com.xmlcalabash.test

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.MediaType
import org.scalatest.flatspec.AnyFlatSpec

class MediaTypeSpec extends AnyFlatSpec {
  private val config = XMLCalabashConfig.newInstance()
  private val text_xml = MediaType.parse("text/xml")
  private val application_xml = MediaType.parse("application/xml")
  private val application_xhtml = MediaType.parse("application/xhtml+xml")
  private val text_html = MediaType.parse("text/html")
  private val application_json = MediaType.parse("application/json")
  private val nothing = MediaType.parse("nothing/nothing")
  private val image_svg = MediaType.parse("image/svg+xml")

  // =============================================

  "text/xml " should " match xml" in {
    assert(text_xml.xmlContentType)
  }

  "text/xml " should " not match html" in {
    assert(!text_xml.htmlContentType)
  }

  "text/xml " should " not match text" in {
    assert(!text_xml.textContentType)
  }

  "text/xml " should " not match json" in {
    assert(!text_xml.jsonContentType)
  }

  "text/xml " should " match any" in {
    assert(text_xml.anyContentType)
  }

  // =============================================

  "application/xml " should " match xml" in {
    assert(application_xml.xmlContentType)
  }

  "application/xml " should " not match html" in {
    assert(!application_xml.htmlContentType)
  }

  "application/xml " should " not match text" in {
    assert(!application_xml.textContentType)
  }

  "application/xml " should " not match json" in {
    assert(!application_xml.jsonContentType)
  }

  "application/xml " should " match any" in {
    assert(application_xml.anyContentType)
  }

  // =============================================

  "application/xhtml " should " not match xml" in {
    assert(!application_xhtml.xmlContentType)
  }

  "application/xhtml " should " match html" in {
    assert(application_xhtml.htmlContentType)
  }

  "application/xhtml " should " not match text" in {
    assert(!application_xhtml.textContentType)
  }

  "application/xhtml " should " not match json" in {
    assert(!application_xhtml.jsonContentType)
  }

  "application/xhtml " should " match any" in {
    assert(application_xhtml.anyContentType)
  }

  // =============================================

  "text/html " should " not match xml" in {
    assert(!text_html.xmlContentType)
  }

  "text/html " should " match html" in {
    assert(text_html.htmlContentType)
  }

  "text/html " should " not match text" in {
    assert(!text_html.textContentType)
  }

  "text/html " should " not match json" in {
    assert(!text_html.jsonContentType)
  }

  "text/html " should " match any" in {
    assert(text_html.anyContentType)
  }

  // =============================================

  "application/json " should " not match xml" in {
    assert(!application_json.xmlContentType)
  }

  "application/json " should " not match html" in {
    assert(!application_json.htmlContentType)
  }

  "application/json " should " not match text" in {
    assert(!application_json.textContentType)
  }

  "application/json " should " match json" in {
    assert(application_json.jsonContentType)
  }

  "application/json " should " match any" in {
    assert(application_json.anyContentType)
  }

  // =============================================

  "image/svg+xml " should " match xml" in {
    assert(image_svg.xmlContentType)
  }

  "image/svg+xml " should " not match html" in {
    assert(!image_svg.htmlContentType)
  }

  "image/svg+xml " should " not match text" in {
    assert(!image_svg.textContentType)
  }

  "image/svg+xml " should " not match json" in {
    assert(!image_svg.jsonContentType)
  }

  "image/svg+xml " should " match any" in {
    assert(image_svg.anyContentType)
  }

  "image/svg+xml " should " match image/*" in {
    assert(image_svg.matches(MediaType.parse("image/*")))
  }

  "image/svg+xml " should " match image/*+xml" in {
    assert(image_svg.matches(MediaType.parse("image/*+xml")))
  }

  "image/svg+xml " should " not match image/foo+xml" in {
    assert(!image_svg.matches(MediaType.parse("image/foo+xml")))
  }

  "image/svg+xml " should " not match image/*" in {
    assert(!image_svg.matches(MediaType.parse("image/png")))
  }
}
