package com.xmlcalabash.util

import java.io.StringReader
import java.net.URI
import javax.xml.transform.stream.StreamSource

class Xslt10Source() {
  private var _source: StreamSource = _

  def source: StreamSource = _source

  def this(document: String, baseURI: URI) = {
    this()
    _source = new StreamSource(new StringReader(document), baseURI.toString)
  }

  def this(document: URI) = {
    this()
    _source = new StreamSource(document.toURL.openStream(), document.toString)
  }
}
