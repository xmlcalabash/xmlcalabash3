package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcMetadata
import net.sf.saxon.s9api.{XdmAtomicValue, XdmMap}
import org.apache.http.client.CookieStore
import org.apache.http.cookie.Cookie
import org.apache.http.impl.client.BasicCookieStore

import java.io.InputStream
import java.net.URI
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

class InternetProtocolResponse(val responseURI: URI) {
  private var _statusCode = Option.empty[Int]
  private var _report = Option.empty[XdmMap]
  private var _headers = Map.empty[String, XdmAtomicValue]
  private var _mediaType = Option.empty[MediaType]
  private val _response = ListBuffer.empty[InputStream]
  private val _responseMetadata = ListBuffer.empty[XProcMetadata]
  private var _cookieStore = Option.empty[CookieStore]

  def statusCode: Option[Int] = _statusCode

  def statusCode_=(value: Int): Unit = {
    _statusCode = Some(value)
  }

  def finalURI: URI = {
    // Because content-disposition might have changed it
    if (_responseMetadata.length == 1 && _responseMetadata.head.baseURI.isDefined) {
      _responseMetadata.head.baseURI.get
    } else {
      responseURI
    }
  }

  def cookieStore: Option[CookieStore] = _cookieStore
  def cookieStore_=(store: CookieStore): Unit = {
    // This is a mutable object, copy it
    _cookieStore = Some(new BasicCookieStore())
    for (cookie <- store.getCookies.asScala) {
      _cookieStore.get.addCookie(cookie)
    }
  }

  def headers: Map[String, XdmAtomicValue] = _headers
  def headers_=(headers: Map[String, XdmAtomicValue]): Unit = {
    _headers = headers
  }

  def report: Option[XdmMap] = _report
  def report_=(map: XdmMap): Unit = {
    _report = Some(map)
  }

  def mediaType: Option[MediaType] = _mediaType

  def mediaType_=(mtype: MediaType): Unit = {
    _mediaType = Some(mtype)
  }

  def addResponse(response: InputStream, meta: XProcMetadata): Unit = {
    _response += response
    _responseMetadata += meta
  }

  def empty: Boolean = _response.isEmpty

  def singlepart: Boolean = !multipart

  def multipart: Boolean = _response.length > 1 || (_mediaType.isDefined && _mediaType.get.mediaType == "multipart")

  def response: ListBuffer[InputStream] = _response

  def responseMetadata: ListBuffer[XProcMetadata] = _responseMetadata
}
