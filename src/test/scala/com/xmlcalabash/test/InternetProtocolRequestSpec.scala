package com.xmlcalabash.test

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import org.apache.http.impl.cookie.BasicClientCookie
import org.scalatest.flatspec.AnyFlatSpec

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.{Calendar, Date}
import scala.jdk.CollectionConverters.ListHasAsScala

class InternetProtocolRequestSpec extends AnyFlatSpec {
  private val config = XMLCalabashConfig.newInstance()

  // =============================================

  "GET " should " return a document" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/fixed-xml"))
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " perform basic authentication" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/basic-auth/"))
    request.authentication("basic", "testuser", "testpassword", true)
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " perform digest authentication" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/digest-auth/"))
    request.authentication("digest", "testuser", "testpassword")
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " return a multipart document" in {
    val request = new InternetProtocolRequest(config,URI.create("http://localhost:8246/service/fixed-multipart"))
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " return the file download URI" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/file-download"))
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
    assert(response.finalURI == URI.create("http://localhost:8246/service/download-test.xml"))
  }

  "GET " should " return URIs for multipart attachments" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/file-multidownload"))
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
    assert(response.finalURI == URI.create("http://localhost:8246/service/file-multidownload"))
    assert(response.response.length == 2)
    assert(response.responseMetadata.length == 2)
    assert(response.responseMetadata.head.baseURI.get == URI.create("http://localhost:8246/service/download-part1.html"))
    assert(response.responseMetadata(1).baseURI.get == URI.create("http://localhost:8246/service/images/download-part2.png"))
  }

  "HEAD " should " succeed" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/"))
    val response = request.execute("HEAD")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "POST " should " succeed" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-post"))
    val meta = new XProcMetadata(MediaType.parse("text/plain"))
    request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
    val response = request.execute("POST")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 201)
  }

  "PUT " should " succeed" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-put"))
    val meta = new XProcMetadata(MediaType.parse("text/plain"))
    request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
    val response = request.execute("PUT")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 202)
  }

  "DELETE " should " succeed" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-delete"))
    val response = request.execute("DELETE")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 204)
  }

  "Cookies " should " be returned" in {
    val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/set-cookies"))
    val response = request.execute("GET")
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
    assert(response.cookieStore.isDefined)
    val cookies = response.cookieStore.get.getCookies.asScala
    assert(cookies.length == 1)
    assert(cookies.head.getName == "One")
    assert(cookies.head.getValue == "1")
  }

  "Cookies " should " be sent" in {
    var request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/set-cookies"))
    var response = request.execute("GET")

    request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/get-cookies"))
    val store = response.cookieStore.get
    store.addCookie(fakeCookie("TestCookie", "test value"))
    request.cookieStore = store
    response = request.execute("GET")
    assert(response != null)

    val docreq = new DocumentRequest(response.finalURI, response.mediaType.get)
    val result = config.documentManager.parse(docreq, response.response.head)
    val xml = result.value.toString
    assert(xml.contains("TestCookie"))
    assert(xml.contains("One"))
  }

  private def fakeCookie(name: String, value: String): BasicClientCookie = {
    val ONEWEEK: Long = 7 * 24 * 60 * 60 * 1000
    val date = Calendar.getInstance().getTime
    val cookie = new BasicClientCookie(name, value)
    cookie.setDomain("localhost")
    cookie.setExpiryDate(new Date(date.getTime + ONEWEEK))
    cookie
  }
}

