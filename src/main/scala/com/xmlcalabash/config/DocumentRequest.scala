package com.xmlcalabash.config

import java.net.URI
import com.jafpl.graph.Location
import com.xmlcalabash.config.DocumentRequest.EMPTY_MAP
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.HashMap

object DocumentRequest {
  val FALSE = new XdmAtomicValue(false)
  val TRUE = new XdmAtomicValue(true)
  val EMPTY_MAP = Map.empty[QName,XdmValue]
  val DTD_VALIDATE = Map(XProcConstants.cx_dtd_validate -> TRUE)
  def dtdValidate(validate: Boolean): Map[QName,XdmValue] = {
    if (validate) {
      DTD_VALIDATE
    } else {
      EMPTY_MAP
    }
  }
}

class DocumentRequest(val href: Option[URI], val contentType: Option[MediaType], val location: Option[Location], val params: Map[QName,XdmValue]) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private var _node = Option.empty[Any]
  private var _nodemeta = Option.empty[XProcMetadata]

  private var _baseURI = Option.empty[URI]
  private var _docprops = Option.empty[Map[QName,XdmValue]]

  def this(href: URI) = {
    this(Some(href), None, None, EMPTY_MAP)
  }

  def this(href: URI, contentType: MediaType) = {
    this(Some(href), Some(contentType), None, EMPTY_MAP)
  }

  def this(href: URI, contentType: MediaType, location: Option[Location]) = {
    this(Some(href), Some(contentType), location, EMPTY_MAP)
  }

  def this(href: URI, contentType: Option[MediaType], location: Option[Location]) = {
    this(Some(href), contentType, location, EMPTY_MAP)
  }

  def this(href: Option[URI], contentType: Option[MediaType], location: Option[Location]) = {
    this(href, contentType, location, EMPTY_MAP)
  }

  def this(node: Any, meta: XProcMetadata, location: Option[Location]) = {
    this(None, Some(meta.contentType), location, EMPTY_MAP)
    _node = Some(node)
    _nodemeta = Some(meta)
    _docprops = Some(meta.properties)
  }

  def node: Option[Any] = _node
  def nodeMetadata: Option[XProcMetadata] = _nodemeta
  def dtdValidate: Boolean = {
    booleanParameter(XProcConstants._dtd_validate).getOrElse(false)
  }

  def booleanParameter(name: QName): Option[Boolean] = {
    if (params.contains(name)) {
      val value = params(name)
      value match {
        case atom: XdmAtomicValue =>
          Some(atom.getBooleanValue)
        case _ =>
          None
      }
    } else {
      None
    }
  }

  def stringParameter(name: QName): Option[String] = {
    if (params.contains(name)) {
      val value = params(name)
      value match {
        case atom: XdmAtomicValue =>
          Some(atom.getStringValue)
        case _ =>
          None
      }
    } else {
      None
    }
  }

  def baseURI: Option[URI] = _baseURI
  def baseURI_=(base: URI): Unit = {
    if (_baseURI.isEmpty) {
      _baseURI = Some(base)
    } else {
      throw new IllegalArgumentException("Only a single assigment to baseURI is allowed")
    }
  }

  def docprops: Map[QName,XdmValue] = {
    if (_docprops.isEmpty) {
      HashMap.empty[QName,XdmValue]
    } else {
      _docprops.get
    }
  }
  def docprops_=(map: Map[QName,XdmValue]): Unit = {
    if (_docprops.isEmpty) {
      _docprops = Some(map)
    } else {
      throw new IllegalArgumentException("Only a single assigment to docprops is allowed")
    }
  }
}
