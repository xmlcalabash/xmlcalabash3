package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.InternetProtocolRequest
import com.xmlcalabash.io.DocumentConverter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.ExpressionEvaluator
import net.sf.saxon.s9api.*
import java.net.URI
import kotlin.math.floor

open class HttpRequestStep(): AbstractAtomicStep() {
    val documents = mutableListOf<XProcDocument>()

    var href: URI = URI("https://xmlcalabash.com/not/used")
    var method = "GET"
    val serialization = mutableMapOf<QName, XdmValue>()
    val headers = mutableMapOf<String, String>()
    val auth = mutableMapOf<String, XdmValue>()
    val parameters = mutableMapOf<QName, XdmValue>()
    var assert = ".?status-code lt 400"

    // Parameters
    private var httpVersion: Pair<Int,Int>? = null
    private var overrideContentType: MediaType? = null
    private var acceptMultipart = true
    private var overrideContentEncoding: String? = null
    private var permitExpiredSslCertificate = false
    private var permitUntrustedSslCertificate = false
    private var followRedirectCount = -1
    private var timeout: Int? = null
    private var failOnTimeout = false
    private var statusOnly = false
    private var suppressCookies = false
    private var sendBodyAnyway = false

    // Authentication
    private var username: String? = null
    private var password: String? = null
    private var authmethod: String? = null
    private var sendauth = false

    override fun run() {
        super.run()
        documents.addAll(queues["source"]!!)

        href = uriBinding(Ns.href)!!
        method = stringBinding(Ns.method)!!
        serialization.putAll(qnameMapBinding(Ns.serialization))
        parameters.putAll(qnameMapBinding(Ns.parameters))
        assert = stringBinding(Ns.assert)!!
        if (options.containsKey(Ns.headers)) {
            headers.putAll(stringMapBinding(Ns.headers))
        }
        if (options.containsKey(Ns.auth)) {
            val value = options[Ns.auth]!!.value
            if (value != XdmEmptySequence.getInstance()) {
                for (key in (value as XdmMap).keySet()) {
                    auth.put(key.stringValue, value.get(key))
                }
            }
        }

        for ((name, value) in parameters) {
            when (name) {
                Ns.httpVersion -> parameterHttpVersion(value)
                Ns.overrideContentType -> parameterOverrideContentType(value)
                Ns.acceptMultipart -> acceptMultipart = booleanParameter(name, value)
                Ns.timeout -> {
                    timeout = integerParameter(name, value)
                    if (timeout!! < 0) {
                        throw stepConfig.exception(XProcError.xcHttpInvalidParameter("timeout", value.toString()))
                    }
                }
                Ns.permitExpiredSslCertificate -> permitExpiredSslCertificate = booleanParameter(name, value)
                Ns.permitUntrustedSslCertificate -> permitUntrustedSslCertificate = booleanParameter(name, value)
                Ns.overrideContentEncoding -> overrideContentEncoding = stringParameter(name, value)
                Ns.followRedirect -> followRedirectCount = integerParameter(name, value)
                Ns.failOnTimeout -> failOnTimeout = booleanParameter(name, value)
                Ns.statusOnly -> statusOnly = booleanParameter(name, value)
                Ns.suppressCookies -> suppressCookies = booleanParameter(name, value)
                Ns.sendBodyAnyway -> sendBodyAnyway = booleanParameter(name, value)
                else -> stepConfig.debug { "Unexpected http-request parameter: ${name}"}
            }
        }

        for ((name, value) in auth) {
            when (name) {
                "username" -> username = stringAuth(name, value)
                "password" -> password = stringAuth(name, value)
                "auth-method" -> authmethod = stringAuth(name, value).lowercase()
                "send-authorization" -> sendauth = booleanAuth(name, value)
                else -> stepConfig.debug { "Unexpected http-request authentication parameter: ${name}"}
            }
        }

        if (password != null && username == null) {
            throw stepConfig.exception(XProcError.xcHttpBadAuth("Username must be specified if password is specified"))
        }

        if (username != null && password == null) {
            password = ""
        }

        if (username != null && authmethod == null) {
            throw stepConfig.exception(XProcError.xcHttpBadAuth("auth-method must be specified"))
        }

        if (authmethod != null && authmethod != "basic" && authmethod != "digest") {
            throw stepConfig.exception(XProcError.xcHttpBadAuth("auth-method must be 'basic' or 'digest'"))
        }

        if (href.scheme == "file") {
            doFile()
        } else if (href.scheme == "http" || href.scheme == "https") {
            doHttp()
        } else {
            throw stepConfig.exception(XProcError.xcHttpUnsupportedScheme(href.scheme))
        }
    }

    private fun doHttp() {
        val request = InternetProtocolRequest(stepConfig, href)
        request.parameters = parameters
        request.timeout = timeout
        request.sendBodyAnyway = sendBodyAnyway
        request.statusOnly = statusOnly
        request.suppressCookies = suppressCookies
        request.httpVersion = httpVersion
        request.followRedirectCount = followRedirectCount
        request.overrideContentType = overrideContentType

        if (username != null) {
            request.authentication(authmethod!!, username!!, password!!, sendauth)
        }

        for (doc in documents) {
            request.addSource(doc)
            if (documents.size == 1) {
                request.properties = doc.properties
            }
        }

        for ((header, value) in headers) {
            request.addHeader(header, value)
        }

        val response = try {
            request.execute(method)
        } catch (ex: XProcException) {
            if (overrideContentType != null) {
                if (ex.error.code == NsErr.xd(57)) {
                    throw stepConfig.exception(XProcError.xcHttpCannotParseAs(overrideContentType!!))
                }
            }
            throw ex
        }

        if (response.multipart && !acceptMultipart) {
            throw stepConfig.exception(XProcError.xcHttpMultipartForbidden(href))
        }

        if (response.statusCode == 408 && failOnTimeout) {
            throw stepConfig.exception(XProcError.xcFailOnTimeout(timeout!!))
        }

        val report = response.report!!

        if (assert != "") {
            val evaluator = ExpressionEvaluator(stepConfig, assert)
            evaluator.setNamespaces(stepConfig.inscopeNamespaces)
            evaluator.setContext(response.report!!)
            val result = evaluator.evaluate()
            if (!result.underlyingValue.effectiveBooleanValue()) {
                val tempdoc = XProcDocument.ofJson(report, stepConfig, MediaType.JSON, DocumentProperties())
                val xmldoc = DocumentConverter(stepConfig, tempdoc, MediaType.XML).convert()
                val xmlReport = xmldoc.value as XdmNode
                throw stepConfig.exception(XProcError.xcHttpAssertionFailed(xmlReport))
            }
        }

        receiver.output("report", XProcDocument(report, stepConfig))

        for (doc in response.response) {
            receiver.output("result", doc)
        }
    }

    private fun doFile() {
        throw stepConfig.exception(XProcError.xcHttpUnsupportedScheme(href.scheme))
    }

    private fun parameterHttpVersion(value: XdmValue) {
        if (value !is XdmAtomicValue || value.typeName != NsXs.string) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameterType("http-version", value.toString()))
        }

        var majVer = 0
        var minVer = 0

        try {
            val verStr = value.stringValue
            val version = verStr.toDouble()
            majVer = floor(version).toInt()

            val pos = verStr.indexOf(".")
            if (pos >= 0) {
                minVer = verStr.substring(pos+1).toInt()
            }

            // I object slightly to this error. In theory, you can send any version you want. Only the
            // server knows if the version is supported.
            if (majVer != 1 || minVer < 0 || minVer > 1) {
                throw stepConfig.exception(XProcError.xcHttpUnsupportedHttpVersion(verStr))
            }
        } catch (ex: XProcException) {
            throw ex
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameter("override-content-type", value.toString()))
        }

        httpVersion = Pair(majVer, minVer)
    }

    private fun parameterOverrideContentType(value: XdmValue) {
        try {
            if (value is XdmAtomicValue && value.typeName == NsXs.string) {
                overrideContentType = MediaType.parse(value.stringValue)
            } else {
                throw stepConfig.exception(XProcError.xcHttpInvalidParameterType("override-content-type", value.toString()))
            }
        } catch (ex: XProcException) {
            throw ex
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameter("override-content-type", value.toString()))
        }
    }

    private fun booleanParameter(name: QName, value: XdmValue): Boolean {
        if (value !is XdmAtomicValue || value.typeName != NsXs.boolean) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameterType(name.localName, value.toString()))
        }
        return value.booleanValue
    }

    private fun stringParameter(name: QName, value: XdmValue): String {
        if (value !is XdmAtomicValue || value.typeName != NsXs.string) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameterType(name.localName, value.toString()))
        }
        return value.stringValue
    }

    private fun integerParameter(name: QName, value: XdmValue): Int {
        if (value !is XdmAtomicValue || value.typeName != NsXs.integer) {
            throw stepConfig.exception(XProcError.xcHttpInvalidParameterType(name.localName, value.toString()))
        }
        return value.stringValue.toInt()
    }

    private fun stringAuth(name: String, value: XdmValue): String {
        if (value !is XdmAtomicValue || value.typeName != NsXs.string) {
            throw stepConfig.exception(XProcError.xcHttpInvalidAuth(name, value.toString()))
        }
        return value.stringValue
    }

    private fun booleanAuth(name: String, value: XdmValue): Boolean {
        if (value !is XdmAtomicValue || value.typeName != NsXs.boolean) {
            throw stepConfig.exception(XProcError.xcHttpInvalidAuth(name, value.toString()))
        }
        return value.booleanValue
    }

    override fun reset() {
        super.reset()
        href = URI("https://xmlcalabash.com/not/used")
        method = "GET"
        headers.clear()
        auth.clear()
        parameters.clear()
        serialization.clear()
        assert = ".?status-code lt 400"
    }

    override fun toString(): String = "p:http-request"
}