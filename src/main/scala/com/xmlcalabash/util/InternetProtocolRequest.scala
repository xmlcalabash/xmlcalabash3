package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CookieStore
import org.apache.http.client.config.{AuthSchemes, CookieSpecs, RequestConfig}
import org.apache.http.client.methods.{HttpDelete, HttpEntityEnclosingRequestBase, HttpGet, HttpHead, HttpPost, HttpPut, HttpRequestBase}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.{FormBodyPartBuilder, HttpMultipartMode, MultipartEntityBuilder}
import org.apache.http.entity.{ByteArrayEntity, ContentType}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{BasicAuthCache, BasicCookieStore, BasicCredentialsProvider, HttpClientBuilder, StandardHttpRequestRetryHandler}
import org.apache.http.{Header, HttpHost, HttpResponse, ProtocolVersion}

import java.io.{ByteArrayInputStream, IOException}
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

class InternetProtocolRequest(val config: XMLCalabashConfig, val context: StaticContext, val uri: URI) {
  private val _expires = new QName("", "expires")
  private val _date = new QName("", "date")
  private val _content_disposition = new QName("", "content-disposition")

  private var builder: HttpClientBuilder = _
  private var httpResult: HttpResponse = _
  private var href: URI = _
  private var finalURI: URI = _
  private val headers = mutable.HashMap.empty[String,String]

  private var _location = Option.empty[Location]
  private var _cookieStore = Option.empty[CookieStore]
  private var _timeout = Option.empty[Int]
  private val _sources = ListBuffer.empty[Array[Byte]] // serialization is your problem, not mine!
  private val _sourcesMetadata = ListBuffer.empty[XProcMetadata]
  private var _authMethod = Option.empty[String]
  private var _authPremptive = false
  private var _usercreds = Option.empty[UsernamePasswordCredentials]
  private var _httpVersion = Option.empty[Tuple2[Int,Int]]
  private var _statusOnly = false
  private var _suppressCookies = false;
  private var _overrideContentType = Option.empty[MediaType]
  private var _followRedirectCount = -1
  private var _sendBodyAnyway = false

  def this(config: XMLCalabashRuntime, context: StaticContext, uri: URI) =
    this(config.config, context, uri)

  def this(config: XMLCalabashConfig, uri: URI) =
    this(config, new StaticContext(config), uri)

  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def cookieStore: Option[CookieStore] = _cookieStore
  def cookieStore_=(store: CookieStore): Unit = {
    // This is a mutable object, copy it
    _cookieStore = Some(new BasicCookieStore())
    for (cookie <- store.getCookies.asScala) {
      _cookieStore.get.addCookie(cookie)
    }
  }

  def timeout: Option[Int] = _timeout
  def timeout_=(timeout: Int): Unit = {
    _timeout = Some(timeout)
  }

  def httpVersion: Option[Tuple2[Int,Int]] = _httpVersion
  def httpVersion_=(version: Tuple2[Int,Int]): Unit = {
    _httpVersion = Some(version)
  }

  def statusOnly: Boolean = _statusOnly
  def statusOnly_=(only: Boolean): Unit = {
    _statusOnly = only
  }

  def suppressCookies: Boolean = _suppressCookies
  def suppressCookies_=(suppress: Boolean): Unit = {
    _suppressCookies = suppress
  }

  def overrideContentType: Option[MediaType] = _overrideContentType
  def overrideContentType_=(mtype: MediaType): Unit = {
    _overrideContentType = Some(mtype.assertValid)
  }

  def followRedirectCount: Int = _followRedirectCount
  def followRedirectCount_=(count: Int): Unit = {
    _followRedirectCount = count
  }

  def sendBodyAnyway: Boolean = _sendBodyAnyway
  def sendBodyAnyway_=(send: Boolean): Unit = {
    _sendBodyAnyway = send
  }

  def addSource(item: Array[Byte], meta: XProcMetadata): Unit = {
    _sources += item;
    _sourcesMetadata += meta;
  }

  def addHeader(name: String, value: String): Unit = {
    if (name.equalsIgnoreCase("content-type")) {
      MediaType.parse(value).assertValid
    }

    if (name.equalsIgnoreCase("transfer-encoding")) {
      throw XProcException.xcUnsupportedTransferEncoding(value, location)
    }

    headers.put(name, value);
  }

  def authentication(method: String, username: String, password: String): Unit = {
    authentication(method, username, password, false);
  }

  def authentication(method: String, username: String, password: String, premptive: Boolean): Unit = {
    if (method != "basic" && method != "digest") {
      throw XProcException.xcHttpBadAuth("auth-method must be 'basic' or 'digest'", location)
    }

    _authMethod = Some(method)
    _usercreds = Some(new UsernamePasswordCredentials(username, password))
    _authPremptive = premptive
  }

  def execute(method: String): InternetProtocolResponse = {
    href = uri
    executeWithRedirects(method)
  }

  private def executeWithRedirects(method: String): InternetProtocolResponse = {
    builder = HttpClientBuilder.create()

    val rqbuilder = RequestConfig.custom()
    rqbuilder.setCookieSpec(CookieSpecs.DEFAULT)
    val localContext = HttpClientContext.create()

    if (cookieStore.isEmpty) {
      cookieStore = new BasicCookieStore()
    }

    builder.setDefaultCookieStore(cookieStore.get)

    if (timeout.isDefined) {
      rqbuilder.setSocketTimeout(timeout.get)
    }

    builder.setDefaultRequestConfig(rqbuilder.build())

    val httpRequest = method.toUpperCase() match {
      case "GET" =>
        setupGetOrHead(method.toUpperCase())
      case "POST" =>
        setupPutOrPost(method.toUpperCase())
      case "PUT" =>
        setupPutOrPost(method.toUpperCase())
      case "HEAD" =>
        setupGetOrHead(method.toUpperCase())
      case "DELETE" =>
        setupDelete()
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported method: $method")
    }

    builder.setRetryHandler(new StandardHttpRequestRetryHandler(3, false))
    for (pscheme <- config.proxies.keySet) {
      val proxy = config.proxies(pscheme)
      val host = new HttpHost(proxy.getHost, proxy.getPort, pscheme)
      builder.setProxy(host)
    }

    //builder.setProxy(new HttpHost("localhost", 8888, "http"))

    if (_usercreds.isDefined) {
      val scope = new AuthScope(uri.getHost, uri.getPort)
      val bCredsProvider = new BasicCredentialsProvider()
      bCredsProvider.setCredentials(scope, _usercreds.get)
      var authpref: List[String] = List("")
      _authMethod.get match {
        case "basic" =>
          authpref = List(AuthSchemes.BASIC)
          if (_authPremptive) {
            // See https://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth
            val authCache = new BasicAuthCache()
            val basicAuth = new BasicScheme()
            authCache.put(new HttpHost(uri.getHost, uri.getPort), basicAuth)
            localContext.setCredentialsProvider(bCredsProvider)
            localContext.setAuthCache(authCache)
          }
        case "digest" =>
          authpref = List(AuthSchemes.DIGEST)
        case _ =>
          throw new RuntimeException("Unexpected authentication method: " + _authMethod.get)
      }

      rqbuilder.setProxyPreferredAuthSchemes(authpref.asJava)
      builder.setDefaultCredentialsProvider(bCredsProvider)
    }

    // We have to do our own redirect handling because (1), we might want to suppress
    // cookies and (2) the semantics of followRedirectCount aren't implemented by
    // the underlying library.
    builder.disableRedirectHandling()
    val httpClient = builder.build()

    if (Option(httpClient).isEmpty) {
      throw new RuntimeException("HTTP requests have been disabled")
    }

    if (httpVersion.isDefined) {
      httpRequest.setProtocolVersion(new ProtocolVersion(href.getScheme, httpVersion.get._1, httpVersion.get._2))
    }

    httpResult = httpClient.execute(httpRequest, localContext)
    finalURI = httpRequest.getURI

    if (List(301, 302, 303).contains(httpResult.getStatusLine.getStatusCode) && (followRedirectCount != 0)) {
      _followRedirectCount -= 1
      val locHeader = Option(httpResult.getFirstHeader("Location"))
      if (locHeader.isDefined) {
        val redirect = new URI(locHeader.get.getValue);
        if (suppressCookies) {
          _cookieStore = None
        }
        href = redirect
        executeWithRedirects(method);
      } else {
        throw new RuntimeException(s"Web server returned ${httpResult.getStatusLine.getStatusCode} without a Location: header")
      }
    }

    val response = new InternetProtocolResponse(finalURI);
    response.statusCode = httpResult.getStatusLine.getStatusCode
    response.cookieStore = cookieStore.get
    response.headers = requestHeaders()
    response.report = requestReport()
    readResponseEntity(response)
  }

  private def readResponseEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
    response.mediaType = getFullContentType

    if (statusOnly) {
      return response
    }

    if (Option(httpResult.getEntity).isEmpty) {
      return response
    }

    if (response.mediaType.get.matches(MediaType.MULTIPART)) {
      readMultipartEntity(response)
    } else {
      readSinglepartEntity(response)
    }
  }

  private def readSinglepartEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
    val entity = httpResult.getEntity
    val length = entity.getContentLength
    val ctype = Option(httpResult.getFirstHeader("content-type"))

    var stream = entity.getContent
    if (length < 0 && ctype.isEmpty) {
      // We have to see if there's any content here...
      try {
        val bytes = stream.readAllBytes()
        if (bytes.isEmpty) {
          // No content-type and no content = no document
          return response
        }
        stream = new ByteArrayInputStream(bytes)
      } catch {
        case ex: IOException =>
          if (ex.getMessage.contains("from closed")) {
            return response
          }
          throw ex
        case ex: Exception =>
          throw ex
      }
    }

    val meta = entityMetadata(httpResult.getAllHeaders.toList)
    response.addResponse(stream, meta)
    response
  }

  private def readMultipartEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
    val contentType = getFullContentType
    val boundary = contentType.paramValue("boundary")

    if (boundary.isEmpty) {
      throw XProcException.xcHttpInvalidBoundary("(none provided)", location)
    }

    val reader = new MIMEReader(httpResult.getEntity.getContent, boundary.get)
    while (reader.readHeaders()) {
      val pctype = reader.header("Content-Type")
      val pclen = reader.header("Content-Length")

      val partStream = if (pclen.isDefined) {
        val len = getHeaderValue(pclen).get.toLong
        reader.readBodyPart(len)
      } else {
        reader.readBodyPart()
      }

      val meta = entityMetadata(reader.getHeaders)

      response.addResponse(partStream, meta)
    }

    response
  }

  private def getHeaderValue(header: Option[Header]): Option[String] = {
    if (header.isEmpty) {
      None
    } else {
      val elems = header.get.getElements
      if (elems == null || elems.isEmpty) {
        None
      } else {
        Some(elems(0).toString)
      }
    }
  }

  private def getFullContentType: MediaType = {
    if (overrideContentType.isDefined) {
      return overrideContentType.get
    }

    val ctype = httpResult.getLastHeader("Content-Type")
    if (Option(ctype).isEmpty) {
      return MediaType.OCTET_STREAM
    }

    val types = ctype.getElements
    if (Option(types).isEmpty || types.isEmpty) {
      return MediaType.OCTET_STREAM
    }

    var params = ""
    for (param <- types(0).getParameters) {
      params += "; "
      params += param.getName.toLowerCase() + "=" + param.getValue
    }

    MediaType.parse(types(0).getName + params)
  }

  private def entityMetadata(headers: List[Header]): XProcMetadata = {
    // If the server sends a content type, we'll use it. So this default
    // only applies if the server doesn't send one. That only happens if
    // the server isn't sending any content or if the server is broken.
    // In the former case, text is more useful. In the latter case, all
    // bets are off anyway.
    var ctype = MediaType.TEXT
    var location = false
    var baseURI = finalURI
    val props = mutable.HashMap.empty[QName, XdmValue]

    for (header <- headers) {
      try {
        val key = new QName("", header.getName.toLowerCase)
        var value = new XdmAtomicValue(header.getValue)

        if (key == _date || key == _expires) {
          try {
            // Convert date time strings into proper xs:dateTime values
            val ta = DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.getValue)
            val dt = Instant.from(ta).toString
            val expr = new XProcXPathExpression(context, s"xs:dateTime('$dt')")
            val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), Map(), None)
            value = smsg.item.asInstanceOf[XdmAtomicValue]
          } catch {
            case _: DateTimeParseException => ()
          }
        }

        if (key == XProcConstants._content_type) {
          if (overrideContentType.isDefined) {
            ctype = overrideContentType.get
            value = new XdmAtomicValue(overrideContentType.get.toString)
          } else {
            ctype = MediaType.parse(header.getValue).discardParams(List("charset"))
            value = new XdmAtomicValue(ctype.toString)
          }
        }

        if (key == _content_disposition) {
          for (helem <- header.getElements) {
            if (helem.getName == "attachment") {
              for (param <- helem.getParameters) {
                if (param.getName == "filename") {
                  baseURI = finalURI.resolve(param.getValue)
                }
              }
            }
          }
        }

        location = location ||  (key == XProcConstants._location)
        props.put(key, value)
      } catch {
        case _: Exception => ()
      }
    }

    if (location) {
      // Lie like a rug. An empty document might as well be text/plain as application/octet-stream
      props.put(XProcConstants._content_type, new XdmAtomicValue("text/plain"))
    }

    props.put(XProcConstants._base_uri, new XdmAtomicValue(baseURI))
    new XProcMetadata(ctype, props.toMap)
  }

  private def requestReport(): XdmMap = {
    var report = new XdmMap()
    report = report.put(new XdmAtomicValue("status-code"), new XdmAtomicValue(httpResult.getStatusLine.getStatusCode))
    report = report.put(new XdmAtomicValue("base-uri"), new XdmAtomicValue(finalURI))

    var headers = new XdmMap()
    for ((name,value) <- requestHeaders()) {
      headers = headers.put(new XdmAtomicValue(name), value)
    }

    report.put(new XdmAtomicValue("headers"), headers)
  }

  private def requestHeaders(): Map[String,XdmAtomicValue] = {
    val headers = mutable.HashMap.empty[String, XdmAtomicValue]

    for (header <- httpResult.getAllHeaders) {
      val key = header.getName.toLowerCase
      if (Option(header.getValue).isDefined) {
        var value = new XdmAtomicValue(header.getValue)
        try {
          if (key.contains("date") || key.contains("modified")) {
            value = new XdmAtomicValue(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.getValue)))
          } else if (key.contains("length")) {
            value = new XdmAtomicValue(Integer.parseInt(header.getValue))
          }
        } catch {
          case _: Throwable =>
            ()
        }
        headers.put(key, value)
      }
    }

    headers.toMap
  }

  private def normalizedHeaders: Map[String,String] = {
    val normHeaders = mutable.HashMap.empty[String,String]

    for ((name,value) <- headers) {
      if (normHeaders.contains(name.toLowerCase)) {
        throw XProcException.xcHttpDuplicateHeader(name.toLowerCase, location)
      }
      normHeaders.put(name.toLowerCase, value)
    }
    normHeaders.clear()

    if (_sources.size == 1) {
      if (_sourcesMetadata.head.property("content-type").isDefined) {
        normHeaders.put("content-type", _sourcesMetadata.head.contentType.toString)
      }
    }

    for ((name,value) <- headers) {
      normHeaders.put(name.toLowerCase, value)
    }

    if (_sources.size == 1) {
      for ((name, value) <- _sourcesMetadata.head.properties) {
        if (name.getNamespaceURI == XProcConstants.ns_chttp) {
          val key = name.getLocalName.toLowerCase
          if (!normHeaders.contains(key)) {
            normHeaders.put(name.getLocalName, value.toString)
          }
        }
      }
    }

    normHeaders.toMap
  }

  private def setupGetOrHead(method: String): HttpRequestBase = {
    val request = if (sendBodyAnyway && _sources.nonEmpty) {
      new HttpWithForcedBody(href, method)
    } else {
      if (method == "HEAD") {
        new HttpHead(href)
      } else {
        new HttpGet(href)
      }
    }

    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
    }

    if (sendBodyAnyway && _sources.nonEmpty) {
      request.asInstanceOf[HttpWithForcedBody].setEntity(new ByteArrayEntity(_sources.head))
    }

    request
  }

  private def setupPutOrPost(method: String): HttpRequestBase = {
    val headers = normalizedHeaders
    var contentType = if (headers.contains("content-type")) {
      if (_sources.size > 1 && !headers("content-type").startsWith("multipart/")) {
        throw XProcException.xcMultipartRequired(headers("content-type"), location)
      }
      MediaType.parse(headers("content-type")).assertValid
    } else {
      if (_sources.size > 1) {
        MediaType.MULTIPART_MIXED
      } else {
        MediaType.OCTET_STREAM
      }
    }

    val request = if (method == "POST") {
      new HttpPost(href)
    } else {
      new HttpPut(href)
    }

    if (contentType.mediaType == "multipart") {
      for ((name,value) <- headers) {
        // The content-type header is used to inform XProc about the desired header, but
        // for multipart, it may be modified (to include a boundary, for example), so
        // don't blindly copy it.
        if (!name.equalsIgnoreCase("content-type")) {
          request.addHeader(name, value)
        }
      }

      val entityBuilder = MultipartEntityBuilder.create()
      entityBuilder.setMode(HttpMultipartMode.STRICT); // FIXME: make a parameter for this?

      if (contentType.paramValue("boundary").isEmpty) {
        contentType = contentType.addParam("boundary", s"B${java.util.UUID.randomUUID().toString}")
      }

      val boundary = contentType.paramValue("boundary").get

      if (boundary.startsWith("--")) {
        throw XProcException.xcHttpInvalidBoundary(boundary, location)
      }

      entityBuilder.setBoundary(boundary)
      entityBuilder.setContentType(ContentType.create(contentType.discardParams().toString))

      for (pos <- _sources.indices) {
        val source = _sources(pos)
        val meta = _sourcesMetadata(pos)

        val bodyContentType = meta.contentType.discardParams()
        val charset = meta.contentType.paramValue("charset").getOrElse(StandardCharsets.UTF_8.toString)

        var part = FormBodyPartBuilder.create()
        part.setName(s"part${pos}")

        var sentDisposition = false
        for ((name,value) <- meta.properties) {
          if (name.getNamespaceURI == XProcConstants.ns_chttp) {
            part = part.addField(name.getLocalName, value.toString)
            sentDisposition = sentDisposition || name.getLocalName.equalsIgnoreCase("content-disposition")
          }
        }
        if (!sentDisposition) {
          part = part.addField("Content-Disposition", "attachment")
        }

        part.setBody(new ByteArrayBody(source, ContentType.create(bodyContentType.toString, charset), null))

        entityBuilder.addPart(part.build())
      }

      request.setEntity(entityBuilder.build())
    } else {
      for ((name,value) <- headers) {
        request.addHeader(name, value)
      }
      if (_sources.nonEmpty) {
        request.setEntity(new ByteArrayEntity(_sources.head))
      }
    }

    request
  }

  private def setupDelete(): HttpRequestBase = {
    val request = if (sendBodyAnyway && _sources.nonEmpty) {
        new HttpWithForcedBody(href, "DELETE")
      } else {
        new HttpDelete(href);
      }

    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
    }

    if (sendBodyAnyway && _sources.nonEmpty) {
      request.asInstanceOf[HttpWithForcedBody].setEntity(new ByteArrayEntity(_sources.head))
    }

    request
  }

  private class HttpWithForcedBody(val uri: URI, val method: String) extends HttpEntityEnclosingRequestBase {
    setURI(uri)
    override def getMethod: String = method.toUpperCase()
  }
}
