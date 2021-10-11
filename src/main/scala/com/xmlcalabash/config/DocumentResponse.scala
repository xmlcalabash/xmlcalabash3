package com.xmlcalabash.config

import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

import scala.collection.immutable.HashMap

class DocumentResponse(val value: XdmValue, val contentType: MediaType, val props: Map[QName,XdmValue]) {
  private var _shadow: Option[Array[Byte]] = None

  def this(value: XdmValue) = {
    this(value, MediaType.OCTET_STREAM, HashMap.empty[QName,XdmValue])
  }

  def this(value: XdmValue, contentType: MediaType) = {
    this(value, contentType, HashMap.empty[QName,XdmValue])
  }

  def this(value: XdmValue, shadow: Array[Byte], contentType: MediaType) = {
    this(value, contentType, HashMap.empty[QName,XdmValue])
    _shadow = Some(shadow)
  }

  def this(value: XdmValue, shadow: Array[Byte], contentType: MediaType, props: Map[QName,XdmValue]) = {
    this(value, contentType, props)
    _shadow = Some(shadow)
  }

  def shadow: Option[Array[Byte]] = _shadow
}
