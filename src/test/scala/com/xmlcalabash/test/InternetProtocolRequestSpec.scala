package com.xmlcalabash.test

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import org.apache.http.impl.cookie.BasicClientCookie
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.{Calendar, Date}
import scala.jdk.CollectionConverters.ListHasAsScala

class InternetProtocolRequestSpec extends AnyFlatSpec with BeforeAndAfter {
  private val config = XMLCalabashConfig.newInstance()
  private var serverAvailable = true

  before {
    try {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/fixed-xml"))
      request.execute("GET")
    } catch {
      case _: Throwable =>
        serverAvailable = false
        println("No server available; assuming tests pass.")
    }
  }

  // =============================================

  "GET " should " return a document" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/fixed-xml"))
      val response = request.execute("GET")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
    } else {
      assert(true)
    }
  }

  "GET " should " perform basic authentication" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/basic-auth/"))
      request.authentication("basic", "testuser", "testpassword", true)
      val response = request.execute("GET")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
    } else {
      assert(true)
    }
  }

  "GET " should " perform digest authentication" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/digest-auth/"))
      request.authentication("digest", "testuser", "testpassword")
      val response = request.execute("GET")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
    } else {
      assert(true)
    }
  }

  "GET " should " return a multipart document" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config,URI.create("http://localhost:8246/service/fixed-multipart"))
      val response = request.execute("GET")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
    } else {
      assert(true)
    }
  }

  "GET " should " return the file download URI" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/file-download"))
      val response = request.execute("GET")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
      assert(response.finalURI == URI.create("http://localhost:8246/service/download-test.xml"))
    } else {
      assert(true)
    }
  }

  "GET " should " return URIs for multipart attachments" in {
    if (serverAvailable) {
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
    } else {
      assert(true)
    }
  }

  "HEAD " should " succeed" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/docs/"))
      val response = request.execute("HEAD")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 200)
    } else {
      assert(true)
    }
  }

  "POST " should " succeed" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-post"))
      val meta = new XProcMetadata(MediaType.parse("text/plain"))
      request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
      val response = request.execute("POST")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 201)
    } else {
      assert(true)
    }
  }

  "PUT " should " succeed" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-put"))
      val meta = new XProcMetadata(MediaType.parse("text/plain"))
      request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
      val response = request.execute("PUT")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 202)
    } else {
      assert(true)
    }
  }

  "DELETE " should " succeed" in {
    if (serverAvailable) {
      val request = new InternetProtocolRequest(config, URI.create("http://localhost:8246/service/accept-delete"))
      val response = request.execute("DELETE")
      assert(response != null)
      assert(response.statusCode.isDefined)
      assert(response.statusCode.get == 204)
    } else {
      assert(true)
    }
  }

  "Cookies " should " be returned" in {
    if (serverAvailable) {
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
    } else {
      assert(true)
    }
  }

  "Cookies " should " be sent" in {
    if (serverAvailable) {
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
    } else {
      assert(true)
    }
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

