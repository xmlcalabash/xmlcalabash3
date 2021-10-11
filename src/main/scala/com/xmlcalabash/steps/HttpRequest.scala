package com.xmlcalabash.steps

import com.jafpl.messages.Message
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}

import java.io.ByteArrayOutputStream
import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class HttpRequest() extends DefaultXmlStep {
  private val _timeout = new QName("", "timeout")

  private val sources = ListBuffer.empty[Any]
  private val sourceMeta = ListBuffer.empty[XProcMetadata]

  private var context: StaticContext = _
  private var href: URI = _
  private var method = ""
  private val headers = mutable.HashMap.empty[String,String]
  private val auth = mutable.HashMap.empty[String, XdmValue]
  private val parameters = mutable.HashMap.empty[QName, XdmValue]
  private var assert = ""

  // Parameters
  private var httpVersion = Option.empty[Tuple2[Int,Int]]
  private var overrideContentType = Option.empty[MediaType]
  private var acceptMultipart = true
  private var overrideContentEncoding = Option.empty[String]
  private var permitExpiredSslCertificate = false
  private var permitUntrustedSslCertificate = false
  private var followRedirectCount = -1
  private var timeout = Option.empty[Integer]
  private var failOnTimeout = false
  private var statusOnly = false
  private var suppressCookies = false
  private var sendBodyAnyway = false

  // Authentication
  private var username = Option.empty[String]
  private var password = Option.empty[String]
  private var authmethod = Option.empty[String]
  private var sendauth = false

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "report" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("application/octet-stream"),
      "report" -> List("application/json"))
  )

  override def reset(): Unit = {
    super.reset()
    href = null
    method = ""
    headers.clear()
    auth.clear()
    parameters.clear()
    assert = ""
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    sources += item
    sourceMeta += metadata
  }

  // FIXME: why is this so different from the other steps?
  override def receiveBinding(variable: NameValueBinding): Unit = {
    val _href = XProcConstants._href
    val _method = XProcConstants._method
    val _serialization = XProcConstants._serialization
    val _headers = new QName("", "headers")
    val _auth = new QName("", "auth")
    val _parameters = XProcConstants._parameters
    val _assert = new QName("", "assert")

    if (variable.value.size() == 0) {
      return
    }

    val value = variable.value
    val context = variable.context

    variable.name match {
      case `_href` =>
        href = if (context.baseURI.isDefined) {
          context.baseURI.get.resolve(value.getUnderlyingValue.getStringValue)
        } else {
          new URI(value.getUnderlyingValue.getStringValue)
        }
      case `_method` =>
        method = value.getUnderlyingValue.getStringValue.toUpperCase
      case `_serialization` =>
        // nop; this his handled elsewhere
      case `_headers` =>
        value match {
          case map: XdmMap =>
            // Grovel through a Java Map
            val iter = map.keySet().iterator()
            while (iter.hasNext) {
              val key = iter.next()
              val value = map.get(key)
              headers.put(key.getStringValue, value.getUnderlyingValue.getStringValue)
            }
          case _ =>
            throw new IllegalArgumentException("The 'headers' option is not a map(xs:string,xs:string)")
        }
      case `_auth` =>
        value match {
          case map: XdmMap =>
            // Grovel through a Java Map
            val iter = map.keySet().iterator()
            while (iter.hasNext) {
              val key = iter.next()
              val value = map.get(key)
              auth.put(key.getStringValue, value)
            }
          case _ =>
            throw new IllegalArgumentException("The 'auth' option is not a map(xs:string,xs:string)")
        }
      case `_parameters` =>
        parameters ++= ValueParser.parseParameters(value, context)
      case `_assert` =>
        assert = value.getUnderlyingValue.getStringValue
      case _ => ()
    }
  }

  override def run(ctx: StaticContext): Unit = {
    super.run(ctx)

    context = ctx

    // Check parameters
    for ((name, value) <- parameters) {
      name match {
        case XProcConstants._http_version =>
          parameterHttpVersion(value)
        case XProcConstants._override_content_type =>
          parameterOverrideContentType(value)
        case XProcConstants._accept_multipart =>
          acceptMultipart = booleanParameter(name.getLocalName, value)
        case XProcConstants._timeout =>
          timeout = Some(integerParameter(name.getLocalName, value))
          if (timeout.get < 0) {
            throw XProcException.xcHttpInvalidParameter(name.getLocalName, value.toString, location)
          }
        case XProcConstants._permit_expired_ssl_certificate =>
          permitExpiredSslCertificate = booleanParameter(name.getLocalName, value)
        case XProcConstants._permit_untrusted_ssl_certificate =>
          permitUntrustedSslCertificate = booleanParameter(name.getLocalName, value)
        case XProcConstants._override_content_encoding =>
          overrideContentEncoding = Some(stringParameter(name.getLocalName, value))
        case XProcConstants._follow_redirect =>
          followRedirectCount = integerParameter(name.getLocalName, value)
        case XProcConstants._fail_on_timeout =>
          failOnTimeout = booleanParameter(name.getLocalName, value)
        case XProcConstants._status_only =>
          statusOnly = booleanParameter(name.getLocalName, value)
        case XProcConstants._suppress_cookies =>
          suppressCookies = booleanParameter(name.getLocalName, value)
        case XProcConstants._send_body_anyway =>
          sendBodyAnyway = booleanParameter(name.getLocalName, value)
        case _ =>
          logger.debug(s"Unexpected http-request parameter: ${name.getLocalName}")
      }
    }

    // Check auth
    for ((name, value) <- auth) {
      name match {
        case "username" =>
          username = Some(stringAuth(name, value))
        case "password" =>
          password = Some(stringAuth(name, value))
        case "auth-method" =>
          authmethod = Some(stringAuth(name, value).toLowerCase)
        case "send-authorization" =>
          sendauth = booleanAuth(name, value)
        case _ =>
          logger.debug(s"Unexpected http-request authentication parameter: $name")
      }
    }

    if (password.isDefined && username.isEmpty) {
      throw XProcException.xcHttpBadAuth("username must be specified if password is specified", location)
    }

    if (username.isDefined) {
      if (password.isEmpty) {
        password = Some("")
      }
    }

    if (username.isDefined && authmethod.isEmpty) {
      throw XProcException.xcHttpBadAuth("auth-method must be specified", location)
    }

    if (authmethod.isDefined) {
      if (authmethod.get != "basic" && authmethod.get != "digest") {
        throw XProcException.xcHttpBadAuth("auth-method must be 'basic' or 'digest'", location)
      }
    }

    if (href.getScheme == "file") {
      doFile()
    } else if (href.getScheme == "http" || href.getScheme == "https") {
      doHttp()
    } else {
      throw XProcException.xcHttpUnsupportedScheme(href, location)
    }
  }

  private def parameterOverrideContentType(value: XdmValue): Unit = {
    try {
      value match {
        case tv: XdmAtomicValue =>
          if (tv.getTypeName == XProcConstants.xs_string) {
            overrideContentType = Some(MediaType.parse(tv.toString))
          } else {
            throw XProcException.xcHttpInvalidParameterType("override-content-type", value.toString, location)
          }
        case _ =>
          throw XProcException.xcHttpInvalidParameter("override-content-type", value.toString, location)
      }
    } catch {
      case ex: XProcException =>
        throw(ex)
      case _: Exception =>
        throw XProcException.xcHttpInvalidParameter("override-content-type", value.toString, location)
    }
  }

  private def parameterHttpVersion(value: XdmValue): Unit = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    var majVer = 0
    var minVer = 0

    try {
      value.toString.toDouble // Ok, it's a number
      var str = value.toString
      if (str.indexOf(".") < 0) {
        str += ".0"
      }
      val pos = str.indexOf(".")
      majVer = str.substring(0, pos).toInt
      minVer = str.substring(pos+1).toInt

      // I object slightly to this error. In theory, you can send any version you want. Only the
      // server knows if the version is supported.
      if (majVer != 1 || minVer < 0 || minVer > 1) {
        throw XProcException.xcHttpUnsupportedHttpVersion(value.toString, location)
      }
    } catch {
      case ex: XProcException =>
        throw ex
      case _: Exception =>
        throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    httpVersion = Some(Tuple2(majVer, minVer))
  }

  private def booleanParameter(name: String, value: XdmValue): Boolean = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_boolean) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.getBooleanValue
  }

  private def integerParameter(name: String, value: XdmValue): Int = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_integer) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.toString.toInt
  }

  private def stringParameter(name: String, value: XdmValue): String = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.toString
  }

  private def stringAuth(name: String, value: XdmValue): String = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    atomicValue.toString
  }

  private def booleanAuth(name: String, value: XdmValue): Boolean = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_boolean) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    atomicValue.getBooleanValue
  }

  private def doHttp(): Unit = {
    val request = new InternetProtocolRequest(config, context, href)

    if (timeout.isDefined) {
      request.timeout = timeout.get
    }

    request.sendBodyAnyway = sendBodyAnyway
    request.statusOnly = statusOnly
    request.suppressCookies = suppressCookies

    if (username.isDefined) {
      request.authentication(authmethod.get, username.get, password.get, sendauth)
    }

    for (pos <- sources.indices) {
      val source = sources(pos)
      val meta = sourceMeta(pos)

      val baos = new ByteArrayOutputStream()
      serialize(context, source, meta, baos)
      request.addSource(baos.toByteArray, meta)
    }

    for ((header,value) <- headers) {
      request.addHeader(header, value)
    }

    if (httpVersion.isDefined) {
      request.httpVersion = httpVersion.get
    }

    request.followRedirectCount = followRedirectCount
    if (overrideContentType.isDefined) {
      request.overrideContentType = overrideContentType.get
    }

    val response = request.execute(method)

    if (response.multipart && !acceptMultipart) {
      throw XProcException.xcHttpMultipartForbidden(href, location)
    }

    val report = response.report.get

    if (assert != "") {
      val msg = new XdmValueItemMessage(report, XProcMetadata.JSON, context)
      val expr = new XProcXPathExpression(context, assert)
      val exeval = config.expressionEvaluator.newInstance()
      val ok = exeval.booleanValue(expr, List(msg), Map.empty[String,Message], None)
      if (!ok) {
        throw XProcException.xcHttpAssertFailed(assert, location)
      }
    }

    consumer.get.receive("report", report, new XProcMetadata(MediaType.JSON))

    for (pos <- response.response.indices) {
      val doc = response.response(pos)
      val meta = response.responseMetadata(pos)

      val request = new DocumentRequest(meta.baseURI, Some(meta.contentType), location)
      val result = try {
        config.documentManager.parse(request, doc)
      } catch {
        case ex: Exception =>
          if (overrideContentType.isDefined) {
            throw XProcException.xcHttpCantInterpret(ex.getMessage, location)
          } else {
            throw ex
          }
      }

      if (result.shadow.isDefined) {
        val node = new BinaryNode(config, result.shadow.get)
        consumer.get.receive("result", node, meta)
      } else {
        consumer.get.receive("result", result.value, meta)
      }
    }
  }

  private def doFile(): Unit = {
    throw XProcException.xcHttpUnsupportedScheme(href, location)
  }
}
