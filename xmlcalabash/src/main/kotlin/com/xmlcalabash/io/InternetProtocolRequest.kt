package com.xmlcalabash.io

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ExpressionEvaluator
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.classic.methods.*
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.cookie.CookieStore
import org.apache.hc.client5.http.entity.EntityBuilder
import org.apache.hc.client5.http.entity.mime.ByteArrayBody
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.auth.BasicAuthCache
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.auth.BasicScheme
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder
import org.apache.hc.core5.util.TimeValue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class InternetProtocolRequest(val stepConfig: XProcStepConfiguration, val uri: URI) {
    companion object {
        private val ns_chttp = NamespaceUri.of("http://www.w3.org/ns/xproc-http")
    }

    private var href: URI? = null
    lateinit private var finalURI: URI
    private val headers = mutableMapOf<String,String>()

    private val _properties = DocumentProperties()
    private val _parameters = mutableMapOf<QName,XdmValue>()

    private var _cookieStore: CookieStore? = null
    var timeout: Int? = null
    private var _sources = mutableListOf<XProcDocument>()
    private var _authMethod: String? = null
    private var _authPreemptive = false
    private var _usercreds: UsernamePasswordCredentials? = null
    var httpVersion: Pair<Int,Int>? = null
    var statusOnly = false
    var suppressCookies = false
    var overrideContentType: MediaType? = null
    var followRedirectCount = -1
    var sendBodyAnyway = false

    var properties: DocumentProperties
        get() = _properties
        set(value) {
            _properties.setAll(value)
        }

    var parameters: Map<QName,XdmValue>
        get() = _parameters
        set(value) {
            _parameters.clear()
            _parameters.putAll(value)
        }

    var cookieStore: CookieStore?
        get() = _cookieStore
        set(value) {
            _cookieStore = BasicCookieStore()
            for (cookie in value!!.cookies) {
                _cookieStore!!.addCookie(cookie)
            }
        }

    fun addSource(doc: XProcDocument) {
        _sources.add(doc)
    }

    fun addHeader(name: String, value: String) {
        if (name.equals("content-type", ignoreCase = true)) {
            MediaType.parse(value)
        }
        if (name.equals("transfer-encoding", ignoreCase = true)) {
            throw stepConfig.exception(XProcError.xcUnsupportedTransferEncoding(value))
        }
        headers.put(name, value)
    }

    fun authentication(method: String, username: String, password: String, preemtive: Boolean = false) {
        if (method != "basic" && method != "digest") {
            throw stepConfig.exception(XProcError.xcHttpBadAuth("auth-method must be 'basic' or 'digest'"))
        }
        _authMethod = method
        _usercreds = UsernamePasswordCredentials(username, password.toCharArray())
        _authPreemptive = preemtive
    }

    fun execute(method: String): InternetProtocolResponse {
        href = uri
        return executeWithRedirects(method)
    }

    private fun executeWithRedirects(method: String): InternetProtocolResponse {
        val builder = HttpClients.custom()
        val rqBuilder = RequestConfig.custom()
        val localContext = HttpClientContext.create()
        if (cookieStore == null) {
            cookieStore = BasicCookieStore()
        }

        if (timeout != null) {
            // https://stackoverflow.com/questions/68096970/httpclient5-lot-of-apis-changed-removed
            // val socketConfig = SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(timeout!!.toLong())).build()
            // val connMgr = BasicHttpClientConnectionManager()
            // connMgr.socketConfig = socketConfig
            // https://stackoverflow.com/questions/78040298/best-way-to-configure-timeouts-on-apache-httpclient-5
            rqBuilder.setConnectionRequestTimeout(timeout!!.toLong(), TimeUnit.SECONDS)
            rqBuilder.setResponseTimeout(timeout!!.toLong(), TimeUnit.SECONDS)
        }

        val httpRequest = when (method.uppercase()) {
            "GET" -> setupGetOrHead(method.uppercase())
            "POST" -> setupPutOrPost(method.uppercase())
            "PUT" -> setupPutOrPost(method.uppercase())
            "HEAD" -> setupGetOrHead(method.uppercase())
            "DELETE" -> setupDelete()
            else -> throw UnsupportedOperationException("Unsupported method: ${method}")
        }

        val requestConfig = rqBuilder.build()
        builder.setDefaultRequestConfig(requestConfig)
        localContext.cookieStore = cookieStore
        builder.setRetryStrategy(DefaultHttpRequestRetryStrategy(3, TimeValue.ofSeconds(1)))

        if (stepConfig.environment.proxies[uri.scheme] != null) {
            builder.setProxy(HttpHost.create(stepConfig.environment.proxies[uri.scheme]))
        }

        if (_usercreds != null) {
            val scope = AuthScope(uri.host, uri.port)
            val bCredsProvider = BasicCredentialsProvider()
            bCredsProvider.setCredentials(scope, _usercreds)

            val authpref = mutableListOf<String>()
            when (_authMethod!!) {
                "basic" -> {
                    authpref.add("basic")
                    if (_authPreemptive) {
                        // See https://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth
                        val authCache = BasicAuthCache()
                        val basicAuth = BasicScheme()
                        authCache.put(HttpHost(uri.host, uri.port), basicAuth)
                        localContext.setCredentialsProvider(bCredsProvider)
                        localContext.setAuthCache(authCache)
                    }
                }
                "digest" -> {
                    authpref.add("digest")
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected authentication method: ${_authMethod}"))
            }

            rqBuilder.setProxyPreferredAuthSchemes(authpref)
            builder.setDefaultCredentialsProvider(bCredsProvider)
        }

        // We have to do our own redirect handling because (1), we might want to suppress
        // cookies and (2) the semantics of followRedirectCount aren't implemented by
        // the underlying library.
        builder.disableRedirectHandling()
        val httpClient = builder
            .build() ?: throw RuntimeException("HTTP requests have been disabled?")

        if (httpVersion != null) {
            localContext.protocolVersion = ProtocolVersion(href!!.scheme, httpVersion!!.first, httpVersion!!.second)
        }

        val responseHandler = ResponseEntityHandler()

        finalURI = httpRequest.uri

        val httpResult = try {
            httpClient.execute(httpRequest, localContext, responseHandler)
        } catch (_: SocketTimeoutException) {
            val response = InternetProtocolResponse(finalURI, 408)
            response.cookieStore = cookieStore
            response.headers = emptyMap()

            var report = XdmMap()
            report = report.put(XdmAtomicValue("status-code"), XdmAtomicValue(408))
            report = report.put(XdmAtomicValue("base-uri"), XdmAtomicValue(finalURI))
            response.report = report

            return response
        }

        if (Thread.currentThread().isInterrupted) {
            throw stepConfig.exception(XProcError.xiThreadInterrupted())
        }

        if (listOf(301, 302, 303).contains(httpResult.code) && (followRedirectCount != 0)) {
            followRedirectCount -= 1
            val locHeader = httpResult.getFirstHeader("Location")
            if (locHeader != null) {
                val redirect = URI(locHeader.value);
                if (suppressCookies) {
                    _cookieStore = null
                }
                href = redirect
                return executeWithRedirects(method);
            } else {
                throw RuntimeException("Web server returned ${httpResult.code} without a Location: header")
            }
        }

        val response = InternetProtocolResponse(finalURI, httpResult.code)
        response.cookieStore = cookieStore
        response.headers = requestHeaders(httpResult)
        response.report = requestReport(httpResult)
        return readResponseEntity(httpResult, response)
    }

    private fun readResponseEntity(httpResult: HttpResponseEntity, response: InternetProtocolResponse): InternetProtocolResponse {
        response.mediaType = getFullContentType(httpResult)

        if (statusOnly) {
            return response
        }

        if (httpResult.body == null) {
            return response
        }

        if (response.mediaType!!.matches(MediaType.MULTIPART)) {
            return readMultipartEntity(httpResult, response)
        } else {
            return readSinglepartEntity(httpResult, response)
        }
    }

    private fun readSinglepartEntity(httpResult: HttpResponseEntity, response: InternetProtocolResponse): InternetProtocolResponse {
        val meta = entityMetadata(httpResult.headers)

        try {
            val stream = ByteArrayInputStream(httpResult.body)
            val docuri = URI(meta[Ns.baseUri]!!.underlyingValue.stringValue)
            val loader = DocumentLoader(stepConfig, docuri, meta, mapOf())
            var doc = loader.load(docuri, stream, response.mediaType!!)

            val serialization = properties[Ns.serialization]
            if (serialization is XdmMap) {
                val encoding = serialization.get(XdmAtomicValue(Ns.encoding))
                if (encoding != null) {
                    val mediaType = response.mediaType!!.addParam("charset", encoding.toString())
                    val mtvalue = XdmAtomicValue(mediaType.toString())
                    val docprop = DocumentProperties(doc.properties)
                    docprop[Ns.contentType] = mtvalue
                    doc = doc.with(docprop)

                    // Haaaaackkkk!!!
                    val headers = response.report!!.get(XdmAtomicValue("headers")) as XdmMap
                    response.report = response.report!!.put(XdmAtomicValue("headers"), headers.put(XdmAtomicValue("content-type"), mtvalue))
                }
            }

            response.addResponse(doc)
            return response
        } catch (ex: SaxonApiException) {
            throw XProcError.xcHttpCannotParseAs(response.mediaType!!).exception(ex)
        }
    }

    private fun readMultipartEntity(httpResult: HttpResponseEntity, response: InternetProtocolResponse): InternetProtocolResponse {
        val contentType = getFullContentType(httpResult)
        val boundary = contentType.paramValue("boundary")

        if (boundary == null) {
            throw stepConfig.exception(XProcError.xcHttpInvalidBoundary("(none provided)"))
        }

        val stream = ByteArrayInputStream(httpResult.body)
        val reader = MimeReader(stream, boundary)
        while (reader.readHeaders()) {
            val pclen = reader.header("Content-Length")

            val partStream = if (pclen != null) {
                val len = getHeaderValue(pclen)!!.toLong()
                reader.readBodyPart(len)
            } else {
                reader.readBodyPart()
            }

            val meta = entityMetadata(reader.headers)
            val baseUri = if (meta.has(Ns.baseUri)) {
                URI(meta[Ns.baseUri].toString())
            } else {
                finalURI
            }
            val mcontentType = if (meta.has(Ns.contentType)) {
                MediaType.parse(meta[Ns.contentType].toString())
            } else {
                MediaType.OCTET_STREAM
            }

            val loader = DocumentLoader(stepConfig, baseUri, meta, mapOf())
            val doc = loader.load(baseUri, partStream, mcontentType)
            response.addResponse(doc)
        }

        return response
    }

    private fun getHeaderValue(header: Header?): String? {
        if (header == null) {
            return null
        } else {
            return header.value
        }
    }

    private fun getFullContentType(httpResult: HttpResponseEntity): MediaType {
        if (overrideContentType != null) {
            return overrideContentType!!
        }

        val ctype = httpResult.getLastHeader("content-type")?.value
        if (ctype == null) {
            if (httpResult.getFirstHeader("location") != null) {
                // Let an empty location response appear to be text
                return MediaType.TEXT
            }
            return MediaType.OCTET_STREAM
        }

        return MediaType.parse(ctype.toString()).discardParameters(listOf("charset"))
    }

    private fun entityMetadata(headers: List<Header>): DocumentProperties {
        // If the server sends a content type, we'll use it. So this default
        // only applies if the server doesn't send one. That only happens if
        // the server isn't sending any content or if the server is broken.
        // In the former case, text is more useful. In the latter case, all
        // bets are off anyway.
        var ctype: MediaType
        var location = false
        var baseURI = finalURI
        val props = DocumentProperties()

        for (header in headers) {
            try {
                val key = QName("", header.name.lowercase())
                var value = XdmAtomicValue(header.value)

                if (key == Ns.date || key == Ns.expires) {
                    try {
                        // Convert date time strings into proper xs:dateTime values
                        val ta = DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.value)
                        val dt = Instant.from(ta).toString()
                        val evaluator = ExpressionEvaluator(stepConfig.processor, "Q{http://www.w3.org/2001/XMLSchema}dateTime('\$dt')")
                        evaluator.setNamespaces(stepConfig.inscopeNamespaces)
                        evaluator.setBindings(mapOf(QName("dt") to XdmAtomicValue(dt)))
                        value = evaluator.evaluate() as XdmAtomicValue
                    } catch (ex: Exception) {
                        // nop
                    }
                }

                if (key == Ns.contentType) {
                    if (overrideContentType != null) {
                        ctype = overrideContentType!!
                        value = XdmAtomicValue(ctype.toString())
                    } else {
                        ctype = MediaType.parse(header.value).discardParameters(listOf("charset"))
                        value = XdmAtomicValue(ctype.toString())
                    }
                }

                if (key == Ns.contentDisposition) {
                    val parts = parseHeader(header.value)
                    if (parts.containsKey("attachment") && parts["attachment"] == null) {
                        if (parts.containsKey("filename")) {
                            baseURI = baseURI!!.resolve(parts["filename"]!!)
                        }
                    }
                }

                location = location || (key == Ns.location)
                if (key != Ns.transferEncoding) {
                    props[key] = value
                }
            } catch (ex: Exception) {
                // nop
            }
        }

        if (location) {
            // Lie like a rug. An empty document might as well be text/plain as application/octet-stream
            props[Ns.contentType] = MediaType.TEXT
        }

        props[Ns.baseUri] = baseURI
        return props
    }

    private fun parseHeader(value: String): Map<String,String?> {
        // This is really crude. It doesn't handle quoted strings at all...
        val parts = mutableMapOf<String,String?>()
        for (part in value.split("\\s*;\\s*".toRegex())) {
            val pos = part.indexOf("=")
            if (pos >= 0) {
                parts[part.substring(0, pos).lowercase()] = part.substring(pos + 1)
            } else {
                parts[part] = null
            }
        }
        return parts
    }

    private fun setupGetOrHead(method: String): ClassicHttpRequest {
        val request = if (sendBodyAnyway && _sources.isNotEmpty()) {
            if (_sources.size != 1) {
                throw stepConfig.exception(XProcError.xiNotImplemented("Sending multiple bodies with GET"))
            }
            if (method == "HEAD") {
                addEntity(ClassicRequestBuilder.head(href), _sources[0]).build()
            } else {
                addEntity(ClassicRequestBuilder.get(href), _sources[0]).build()
            }
        } else {
            if (method == "HEAD") {
                HttpHead(href)
            } else {
                HttpGet(href)
            }
        }

        for ((name, value) in normalizedHeaders()) {
            request.addHeader(name, value)
        }

        return request
    }

    private fun setupPutOrPost(method: String): HttpUriRequestBase {
        val headers = normalizedHeaders()
        var contentType = if (headers.contains("content-type")) {
            if (_sources.size > 1 && !headers["content-type"]!!.startsWith("multipart/")) {
                throw stepConfig.exception(XProcError.xcMultipartRequired(headers["content-type"]!!))
            }
            MediaType.parse(headers["content-type"]!!)
        } else {
            if (_sources.size > 1) {
                MediaType.MULTIPART_MIXED
            } else {
                MediaType.OCTET_STREAM
            }
        }

        val request = if (method == "POST") {
            HttpPost(href)
        } else {
            HttpPut(href)
        }

        if (contentType.mediaType == "multipart") {
            for ((name,value) in headers) {
                // The content-type header is used to inform XProc about the desired header, but
                // for multipart, it may be modified (to include a boundary, for example), so
                // don't blindly copy it.
                if (name != "content-type") {
                    request.addHeader(name, value)
                }
            }

            val entityBuilder = MultipartEntityBuilder.create()
            entityBuilder.setMode(HttpMultipartMode.STRICT); // FIXME: make a parameter for this?

            if (contentType.paramValue("boundary") == null) {
                contentType = contentType.addParam("boundary", "B${java.util.UUID.randomUUID()}")
            }

            val boundary = contentType.paramValue("boundary")!!

            if (boundary.startsWith("--")) {
                throw stepConfig.exception(XProcError.xcHttpInvalidBoundary(boundary))
            }

            entityBuilder.setBoundary(boundary)
            entityBuilder.setContentType(ContentType.create(contentType.discardParameters().toString()))

            for (pos in _sources.indices) {
                val source = _sources[pos]

                val bodyContentType = source.contentType!!.discardParameters()
                val charset = source.contentType?.paramValue("charset") ?: StandardCharsets.UTF_8.toString()

                var part = FormBodyPartBuilder.create()
                part.setName("part${pos}")

                var sentDisposition = false
                for ((name,value) in source.properties.asMap()) {
                    if (name.namespaceUri == ns_chttp) {
                        part = part.addField(name.localName, value.toString())
                        sentDisposition = sentDisposition || name.localName.equals("content-disposition", ignoreCase = true)
                    }
                }
                if (!sentDisposition) {
                    part = part.addField("Content-Disposition", "attachment")
                }

                val baos = ByteArrayOutputStream()
                val serializer = XProcSerializer(stepConfig)
                serializer.write(source, baos, "HTTP request")

                part.setBody(ByteArrayBody(baos.toByteArray(), ContentType.create(bodyContentType.toString(), charset)))

                entityBuilder.addPart(part.build())
            }

            request.setEntity(entityBuilder.build())
        } else {
            for ((name,value) in headers) {
                request.addHeader(name, value)
            }
            if (_sources.isNotEmpty()) {
                val charset = _sources[0].contentType?.paramValue("charset") ?: StandardCharsets.UTF_8.toString()

                val baos = ByteArrayOutputStream()
                val serializer = XProcSerializer(stepConfig)
                serializer.write(_sources[0], baos, "HTTP request")

                // This is a bit awkward.
                // https://github.com/ndw/xmlcalabash1/issues/290
                // https://stackoverflow.com/questions/46076359/adding-content-id-header-in-multipart-entity
                val paramList = mutableListOf<NameValuePair>()
                for (param in contentType.parameters) {
                    paramList.add(param)
                }
                val paramArr = paramList.toTypedArray()

                request.setEntity(ByteArrayEntity(baos.toByteArray(), ContentType.create(contentType.toStringWithoutParameters(), *paramArr)))
            }
        }

        return request
    }

    private fun setupDelete(): ClassicHttpRequest {
        val request = if (sendBodyAnyway && _sources.isNotEmpty()) {
            if (_sources.size != 1) {
                throw stepConfig.exception(XProcError.xiNotImplemented("Sending multiple bodies with GET"))
            }
            addEntity(ClassicRequestBuilder.delete(href), _sources[0]).build()
        } else {
            HttpDelete(href)
        }

        for ((name, value) in normalizedHeaders()) {
            request.addHeader(name, value)
        }

        return request
    }

    private fun addEntity(builder: ClassicRequestBuilder, doc: XProcDocument): ClassicRequestBuilder {
        val ebuilder = EntityBuilder.create()

        val baos = ByteArrayOutputStream()
        val serializer = XProcSerializer(stepConfig)
        serializer.write(doc, baos, "HTTP request")
        ebuilder.setBinary(baos.toByteArray())

        val docctype = doc.contentType ?: MediaType.OCTET_STREAM
        val ctype = ContentType.create(docctype.toString(), StandardCharsets.UTF_8)
        ebuilder.setContentType(ctype)

        return builder.setEntity(ebuilder.build())
    }

    private fun requestReport(httpResult: HttpResponseEntity): XdmMap {
        var report = XdmMap()
        report = report.put(XdmAtomicValue("status-code"), XdmAtomicValue(httpResult.code))
        report = report.put(XdmAtomicValue("base-uri"), XdmAtomicValue(finalURI!!))

        var headers = XdmMap()
        for ((name, value) in requestHeaders(httpResult)) {
            headers = headers.put(XdmAtomicValue(name), value)
        }
        report = report.put(XdmAtomicValue("headers"), headers)
        return report
    }

    private fun requestHeaders(httpResult: HttpResponseEntity): Map<String,XdmAtomicValue> {
        val headers = mutableMapOf<String,XdmAtomicValue>()

        for (header in httpResult.headers) {
            val key = header.name.lowercase()
            if (header.value != null) {
                var value = XdmAtomicValue(header.value)
                try {
                    if (key.contains("date") || key.contains("modified")) {
                        value = XdmAtomicValue(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.value)))
                    } else if (key.contains("length")) {
                        value = XdmAtomicValue(Integer.parseInt(header.value))
                    }
                } catch (ex: Exception) {
                    // ignore it
                }
                headers[key] = value
            }
        }

        return headers
    }

    private fun normalizedHeaders(): Map<String,String> {
        val normHeaders = mutableMapOf<String,String>()
        for ((name, value) in headers) {
            if (normHeaders.containsKey(name.lowercase())) {
                throw stepConfig.exception(XProcError.xcHttpDuplicateHeader(name.lowercase()))
            }
            normHeaders[name.lowercase()] = value
        }
        if (_sources.size == 1) {
            val ctype = _sources[0].contentType
            if (ctype != null && !normHeaders.containsKey("content-type")) {
                normHeaders["content-type"] = ctype.toString()
            }
            for ((name, value) in _sources[0].properties.asMap()) {
                if (name.namespaceUri == ns_chttp) {
                    val key = name.localName.lowercase()
                    if (!normHeaders.containsKey(key)) {
                        normHeaders[key] = value.toString()
                    }
                }
            }
        }

        return normHeaders
    }

    internal class ResponseEntityHandler(): HttpClientResponseHandler<HttpResponseEntity> {
        override fun handleResponse(response: ClassicHttpResponse?): HttpResponseEntity {
            val entity = HttpResponseEntity()
            if (response == null) {
                return entity
            }
            entity.code = response.code
            entity.headers.addAll(response.headers)
            if (response.entity != null) {
                entity.body = response.entity.content.readAllBytes()
            }
            return entity
        }
    }

    internal class HttpResponseEntity() {
        var code = -1
        val headers = mutableListOf<Header>()
        var body: ByteArray? = null

        fun getFirstHeader(name: String): Header? {
            for (header in headers) {
                if (name.equals(header.name, ignoreCase = true)) {
                    return header
                }
            }
            return null
        }

        fun getLastHeader(name: String): Header? {
            var lastHeader: Header? = null
            for (header in headers) {
                if (name.equals(header.name, ignoreCase = true)) {
                    lastHeader = header
                }
            }
            return lastHeader
        }
    }
}