package com.xmlcalabash.steps.pagedmedia.prince

import com.princexml.wrapper.Prince
import com.princexml.wrapper.enums.AuthMethod
import com.princexml.wrapper.enums.AuthScheme
import com.princexml.wrapper.enums.InputType
import com.princexml.wrapper.enums.KeyBits
import com.princexml.wrapper.enums.PdfEvent
import com.princexml.wrapper.enums.PdfProfile
import com.princexml.wrapper.enums.SslType
import com.princexml.wrapper.enums.SslVersion
import com.princexml.wrapper.events.MessageType
import com.princexml.wrapper.events.PrinceEvents
import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssPrince: CssProcessor {
    companion object {
        val _exePath = QName("exePath")

        val _allowAssembly = QName("allowAssembly")
        val _allowPrint = QName("allowPrint")
        val _allowModify = QName("allowModify")
        val _allowCopy = QName("allowCopy")
        val _allowCopyForAccessibility = QName("allowCopyForAccessibility")
        val _allowAnnotate = QName("allowAnnotate")
        val _artificialFonts = QName("artificialFonts")
        val _attachments = QName("attachments")
        val _authorStyle = QName("authorStyle")
        val _authUser = QName("authUser")
        val _authMethods = QName("authMethods")
        val _authPassword = QName("authPassword")
        val _authScheme = QName("authServer")
        val _authServer = QName("authServer")
        val _authPreemptive = QName("authPreemptive")
        val _baseURL = QName("baseURL")
        val _compress = QName("compress")
        val _cookies = QName("cookies")
        val _cookieJar = QName("cookieJar")
        val _convertColors = QName("convertColors")
        val _debug = QName("debug")
        val _defaultStyle = QName("defaultStyle")
        val _embedFonts = QName("embedFonts")
        val _encrypt = QName("encrypt")
        val _failDroppedContent = QName("failDroppedContent")
        val _failInvalidLicense = QName("failInvalidLicense")
        val _failMissingResources = QName("failMissingResources")
        val _failMissingGlyphs = QName("failMissingGlyphs")
        val _failPdfProfileError = QName("failPdfProfileError")
        val _failPdfTagError = QName("failPdfTagError")
        val _failSafe = QName("failSafe")
        val _failStrippedTransparency = QName("failStrippedTransparency")
        val _fallbackCmykProfile = QName("fallbackCmykProfile")
        val _forceIdentityEncoding = QName("forceIdentityEncoding")
        val _httpProxy = QName("httpProxy")
        val _httpTimeout = QName("httpTimeout")
        val _iframes = QName("iframes")
        val _inputType = QName("inputType")
        val _javascript = QName("javascript")
        val _keyBits = QName("keyBits")
        val _licenseFile = QName("licenseFile")
        val _licenseKey = QName("licenseKey")
        val _log = QName("log")
        val _network = QName("network")
        val _objectStreams = QName("objectStreams")
        val _ownerPassword = QName("ownerPassword")
        val _redirects = QName("redirects")
        val _maxPasses = QName("maxPasses")
        val _media = QName("media")
        val _parallelDownloads = QName("noParallelDownloads")
        val _pdfEventScripts = QName("pdfEventScripts")
        val _pdfId = QName("pdfId")
        val _pdfForms = QName("pdfForms")
        val _pdfLang = QName("pdfLang")
        val _pdfProfile = QName("pdfProfile")
        val _pdfOutputIntent = QName("pdfOutputIntent")
        val _pdfScript = QName("pdfScript")
        val _pdfTitle = QName("pdfTitle")
        val _pdfSubject = QName("pdfSubject")
        val _pdfAuthor = QName("pdfAuthor")
        val _pdfKeywords = QName("pdfKeywords")
        val _pdfCreator = QName("pdfCreator")
        val _scripts = QName("scripts")
        val _sslCaCert = QName("sslCaCert")
        val _sslCaPath = QName("sslCaPath")
        val _sslCert = QName("sslCert")
        val _sslCertType = QName("sslCertType")
        val _sslKey = QName("sslKey")
        val _sslKeyType = QName("sslKeyType")
        val _sslKeyPassword = QName("sslKeyPassword")
        val _sslVersion = QName("sslVersion")
        val _sslVerification = QName("sslVerification")
        val _subsetFonts = QName("subsetFonts")
        // val _stylesheets = QName("stylesheets") These arrive on the stylesheet port...
        val _taggedPdf = QName("taggedPdf")
        val _userPassword = QName("userPassword")
        val _verbose = QName("verbose")
        val _warnCssUnknown = QName("noWarnCssUnknown")
        val _warnCssUnsupported = QName("noWarnCssUnsupported")
        val _xinclude = QName("xinclude")
        val _xmlExternalEntities = QName("xmlExternalEntities")
        val _xmp = QName("xmp")

        val stringOptions = listOf(_authPassword, _authScheme, _authServer, _authUser, _baseURL, _cookieJar,
            _fallbackCmykProfile, _httpProxy, _httpTimeout, _inputType, _keyBits,
            _licenseFile, _licenseKey, _log, _maxPasses, _media, _ownerPassword, _pdfAuthor,
            _pdfCreator, _pdfId, _pdfKeywords, _pdfLang, _pdfOutputIntent, _pdfProfile,
            _pdfScript, _pdfSubject, _pdfTitle, _sslCaCert, _sslCaPath, _sslCert,
            _sslCertType, _sslKey, _sslKeyPassword, _sslKeyType, _sslVersion, _userPassword,
            _xmp, _attachments, _authMethods, _cookies, _pdfEventScripts, _scripts)
        val booleanOptions = listOf(_allowAnnotate, _allowAssembly, _allowCopy, _allowCopyForAccessibility,
            _allowModify, _allowPrint, _artificialFonts, _authPreemptive, _authorStyle,
            _compress, _convertColors, _debug, _defaultStyle, _embedFonts, _encrypt,
            _failDroppedContent, _failInvalidLicense, _failMissingGlyphs,
            _failMissingResources, _failPdfProfileError, _failPdfTagError, _failSafe,
            _failStrippedTransparency, _forceIdentityEncoding, _iframes, _javascript,
            _network, _objectStreams, _parallelDownloads, _pdfForms, _redirects,
            _sslVerification, _subsetFonts, _taggedPdf, _verbose, _warnCssUnknown,
            _warnCssUnsupported, _xinclude, _xmlExternalEntities)

        val defaultBooleanOptions = mutableMapOf<QName,Boolean>()
        val defaultStringOptions = mutableMapOf<QName,String>()

        var setInputType = false

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != PrinceManager.princeCssFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported Prince property: ${key}")
                }
            }

            if (defaultStringOptions[_exePath] == null) {
                val prop = System.getProperty("com.xmlcalabash.css.prince.exepath")
                if (prop != null) {
                    defaultStringOptions[_exePath] = prop
                } else {
                    val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
                        "prince.exe"
                    } else {
                        "prince"
                    }

                    var found: String? = null
                    for (path in System.getenv("PATH").split(File.pathSeparator)) {
                        val exe = Paths.get(path, exeName).toFile()
                        if (exe.exists() && exe.canExecute()) {
                            found = exe.absolutePath
                            break
                        }
                    }
                    if (found != null) {
                        defaultStringOptions[_exePath] = found
                    }
                }
            }
        }
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var options: Map<QName, XdmValue>
    lateinit var prince: Prince
    var primarySS: String? = null
    val userSS = mutableListOf<String>()
    val tempFiles = mutableListOf<File>()

    override fun name(): String {
        return "Prince XML"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options
        primarySS = null
        userSS.clear()
        tempFiles.clear()

        val exePath = options[_exePath]?.underlyingValue?.stringValue ?: defaultStringOptions[_exePath] ?: ""
        if (exePath == "") {
            throw stepConfig.exception(XProcError.xdStepFailed("Cannot find Prince XML executable"))
        }

        prince = Prince(exePath, PrinceMessages())
        setInputType = false

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _attachments -> {
                        for (attachment in value.trim().split("\\s+".toRegex())) {
                            prince.addFileAttachment(attachment)
                        }
                    }
                    _authPassword -> prince.setAuthPassword(value)
                    _authMethods -> {
                        for (method in value.trim().split("\\s+".toRegex())) {
                            when (method) {
                                "basic" -> AuthMethod.BASIC
                                "digest" -> AuthMethod.DIGEST
                                "ntlm" -> AuthMethod.NTLM
                                "negotiate" -> AuthMethod.NEGOTIATE
                                else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported auth method: ${method}"))
                            }
                        }
                    }
                    _authScheme -> {
                        when (value) {
                            "http" -> prince.setAuthScheme(AuthScheme.HTTP)
                            "https" -> prince.setAuthScheme(AuthScheme.HTTPS)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported auth scheme: ${value}"))
                        }
                    }
                    _authServer -> prince.setAuthServer(value)
                    _authUser -> prince.setAuthUser(value)
                    _baseURL -> prince.setBaseUrl(value)
                    _cookies -> {
                        for (cookie in value.trim().split("\\s+".toRegex())) {
                            prince.addCookie(cookie)
                        }
                    }
                    _cookieJar -> prince.setCookieJar(value)
                    _fallbackCmykProfile -> prince.setFallbackCmykProfile(value)
                    _httpProxy -> prince.setHttpProxy(value)
                    _httpTimeout -> prince.setHttpTimeout(value.toInt())
                    _inputType -> {
                        setInputType = true
                        when (value) {
                            "auto" -> setInputType = false // This doesn't work with the API we're using
                            "html" -> prince.setInputType(InputType.HTML)
                            "xml" -> prince.setInputType(InputType.XML)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported input type: ${value}"))
                        }
                    }
                    _keyBits -> {
                        when (value) {
                            "40" -> prince.setKeyBits(KeyBits.BITS40)
                            "128" -> prince.setKeyBits(KeyBits.BITS128)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported key bits: ${value}"))
                        }
                    }
                    _licenseFile -> prince.setLicenseFile(value)
                    _licenseKey -> prince.setLicenseKey(value)
                    _log -> prince.setLog(value)
                    _media -> prince.setMedia(value)
                    _maxPasses -> prince.setMaxPasses(value.toInt())
                    _ownerPassword -> prince.setOwnerPassword(value)
                    _pdfAuthor -> prince.setPdfAuthor(value)
                    _pdfCreator -> prince.setPdfCreator(value)
                    _pdfEventScripts -> {
                        val pairs = value.trim().split("\\s+".toRegex())
                        if (pairs.size % 2 != 0) {
                            throw stepConfig.exception(XProcError.xdStepFailed("pdfEventScripts must be a set of pairs: ${value}"))
                        }
                        var index = 0
                        while (index < pairs.size) {
                            val event = pairs[index]
                            val script = pairs[index+1]
                            index += 2
                            when (event) {
                                "will-close" -> prince.addPdfEventScript(PdfEvent.WILL_CLOSE, script)
                                "will-save" -> prince.addPdfEventScript(PdfEvent.WILL_SAVE, script)
                                "did-save" -> prince.addPdfEventScript(PdfEvent.DID_SAVE, script)
                                "will-print" -> prince.addPdfEventScript(PdfEvent.WILL_PRINT, script)
                                "did-print" -> prince.addPdfEventScript(PdfEvent.DID_PRINT, script)
                                else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported PDF event: ${event}"))
                            }
                        }
                    }
                    _pdfId -> prince.setPdfId(value)
                    _pdfKeywords -> prince.setPdfKeywords(value)
                    _pdfLang -> prince.setPdfLang(value)
                    _pdfOutputIntent -> prince.setPdfOutputIntent(value)
                    _pdfProfile -> {
                        when (value) {
                            "PDF/A-1a" -> prince.setPdfProfile(PdfProfile.PDFA_1A)
                            "PDF/A-1a+PDF/UA-1" -> prince.setPdfProfile(PdfProfile.PDFA_1A_AND_PDFUA_1)
                            "PDF/A-1b" -> prince.setPdfProfile(PdfProfile.PDFA_1B)
                            "PDF/A-2a" -> prince.setPdfProfile(PdfProfile.PDFA_2A)
                            "PDF/A-2a+PDF/UA-1" -> prince.setPdfProfile(PdfProfile.PDFA_2A_AND_PDFUA_1)
                            "PDF/A-2b" -> prince.setPdfProfile(PdfProfile.PDFA_2B)
                            "PDF/A-3a" -> prince.setPdfProfile(PdfProfile.PDFA_3A)
                            "PDF/A-3a+PDF/UA-1" -> prince.setPdfProfile(PdfProfile.PDFA_3A_AND_PDFUA_1)
                            "PDF/A-3b" -> prince.setPdfProfile(PdfProfile.PDFA_3B)
                            "PDF/UA-1" -> prince.setPdfProfile(PdfProfile.PDFUA_1)
                            "PDF/X-1a:2001" -> prince.setPdfProfile(PdfProfile.PDFX_1A_2001)
                            "PDF/X-1a:2003" -> prince.setPdfProfile(PdfProfile.PDFX_1A_2003)
                            "PDF/X-3:2002" -> prince.setPdfProfile(PdfProfile.PDFX_3_2002)
                            "PDF/X-3:2003" -> prince.setPdfProfile(PdfProfile.PDFX_3_2003)
                            "PDF/X-4" -> prince.setPdfProfile(PdfProfile.PDFX_4)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported PDF profile: ${value}"))
                        }
                    }
                    _pdfSubject -> prince.setPdfSubject(value)
                    _pdfTitle -> prince.setPdfTitle(value)
                    _pdfScript -> prince.setPdfScript(value)
                    _scripts -> {
                        for (script in value.trim().split("\\s+".toRegex())) {
                            prince.addScript(script)
                        }
                    }
                    _sslCaCert -> prince.setSslCaCert(value)
                    _sslCaPath -> prince.setSslCaPath(value)
                    _sslCert -> prince.setSslCert(value)
                    _sslCertType -> {
                        when (value) {
                            "pem" -> prince.setSslCertType(SslType.PEM)
                            "der" -> prince.setSslCertType(SslType.DER)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported SSL certificate type: ${value}"))
                        }
                    }
                    _sslKey -> prince.setSslKey(value)
                    _sslKeyType -> {
                        when (value) {
                            "pem" -> prince.setSslCertType(SslType.PEM)
                            "der" -> prince.setSslCertType(SslType.DER)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported SSL key type: ${value}"))
                        }
                    }
                    _sslKeyPassword -> prince.setSslKeyPassword(value)
                    _sslVersion -> {
                        when (value) {
                            "default" -> prince.setSslVersion(SslVersion.DEFAULT)
                            "tlsv1" -> prince.setSslVersion(SslVersion.TLSV1)
                            "tlsv1.0" -> prince.setSslVersion(SslVersion.TLSV1_0)
                            "tlsv1.1" -> prince.setSslVersion(SslVersion.TLSV1_1)
                            "tlsv1.2" -> prince.setSslVersion(SslVersion.TLSV1_2)
                            "tlsv1.3" -> prince.setSslVersion(SslVersion.TLSV1_3)
                            else -> throw stepConfig.exception(XProcError.xdStepFailed("Unsupported SSL version: ${value}"))
                        }
                    }
                    _userPassword -> prince.setUserPassword(value)
                    _xmp -> prince.setXmp(value)
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _allowAssembly -> prince.setAllowAssembly(value)
                    _allowAnnotate -> prince.setDisallowAnnotate(!value) // inverted
                    _allowCopy -> prince.setDisallowCopy(!value) // inverted
                    _allowCopyForAccessibility -> prince.setAllowCopyForAccessibility(value)
                    _allowModify -> prince.setDisallowModify(!value) // inverted
                    _allowPrint -> prince.setDisallowPrint(!value) // inverted
                    _artificialFonts -> prince.setNoArtificialFonts(!value) // inverted
                    _authorStyle -> prince.setNoAuthorStyle(!value) // inverted
                    _authPreemptive -> prince.setNoAuthPreemptive(!value) // inverted
                    _compress -> prince.setNoCompress(!value) // inverted
                    _convertColors -> prince.setConvertColors(value)
                    _debug -> prince.setDebug(value)
                    _defaultStyle -> prince.setNoDefaultStyle(!value) // inverted
                    _embedFonts -> prince.setNoEmbedFonts(!value) // inverted
                    _encrypt -> prince.setEncrypt(value)
                    _failDroppedContent -> prince.setFailDroppedContent(value)
                    _failInvalidLicense -> prince.setFailInvalidLicense(value)
                    _failMissingGlyphs -> prince.setFailMissingGlyphs(value)
                    _failMissingResources -> prince.setFailMissingResources(value)
                    _failPdfProfileError -> prince.setFailPdfProfileError(value)
                    _failPdfTagError -> prince.setFailPdfTagError(value)
                    _failStrippedTransparency -> prince.setFailStrippedTransparency(value)
                    _failSafe -> prince.setFailSafe(value)
                    _forceIdentityEncoding -> prince.setForceIdentityEncoding(value)
                    _iframes -> prince.setIframes(value)
                    _javascript -> prince.setJavaScript(value)
                    _network -> prince.setNoNetwork(!value) // inverted!
                    _objectStreams -> prince.setNoObjectStreams(!value) // inverted
                    _parallelDownloads -> prince.setNoParallelDownloads(!value) //inverted
                    _pdfForms -> prince.setPdfForms(value)
                    _redirects -> prince.setNoRedirects(!value) // inverted
                    _sslVerification -> prince.setInsecure(!value) // inverted
                    _subsetFonts -> prince.setNoSubsetFonts(!value) // inverted
                    _taggedPdf -> prince.setTaggedPdf(value)
                    _verbose -> prince.setVerbose(value)
                    _warnCssUnknown -> prince.setNoWarnCssUnknown(!value) // inverted
                    _warnCssUnsupported -> prince.setNoWarnCssUnsupported(!value) // inverted
                    _xinclude -> prince.setXInclude(value)
                    _xmlExternalEntities -> prince.setXmlExternalEntities(value)
                    else -> Unit
                }
            }
        }
    }

    private fun addStylesheet(uri: URI) {
        if (primarySS == null) {
            primarySS = uri.toString()
        } else {
            userSS.add(uri.toString())
        }
    }

    override fun addStylesheet(document: XProcDocument) {
        if (document.contentClassification != MediaClassification.TEXT) {
            stepConfig.error { "Ignoring non-text CSS sytlesheet: ${document.baseURI}" }
            return
        }

        val temp = File.createTempFile("xmlcalabash-princecss", ".css")
        temp.deleteOnExit()
        tempFiles.add(temp)

        stepConfig.debug { "css-formatter css: ${temp.absolutePath}" }

        val cssout = PrintStream(temp)
        cssout.print(document.value.underlyingValue.stringValue)
        cssout.close()

        addStylesheet(temp.toURI())
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (contentType != MediaType.PDF) {
            throw stepConfig.exception(XProcError.xcUnsupportedContentType(contentType))
        }

        if (!setInputType) {
            if (contentType.classification() == MediaClassification.XML) {
                prince.setInputType(InputType.XML)
            } else {
                prince.setInputType(InputType.HTML)
            }
        }

        if (primarySS != null) {
            prince.addStyleSheet(primarySS)
        }

        for (css in userSS) {
            prince.addStyleSheet(css)
        }

        val tempXml = File.createTempFile("xmlcalabash-princecss", ".xml")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        stepConfig.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val fos = FileOutputStream(tempXml)
        DocumentWriter(document, fos).write()
        fos.close()

        val fis = FileInputStream(tempXml)
        prince.convert(fis, out)
        fis.close()

        while (tempFiles.isNotEmpty()) {
            val temp = tempFiles.removeAt(0)
            try {
                temp.delete()
            } catch (ex: Exception) {
                // nop
            }
        }
    }

    inner class PrinceMessages: PrinceEvents {
        override fun onMessage(msgType: MessageType?, msgLocation: String?, message: String?) {
            when (msgType) {
                MessageType.DBG -> stepConfig.debug { message ?: "Prince message event without message?" }
                MessageType.INF -> stepConfig.info { message ?: "Prince message event without message?" }
                MessageType.WRN -> stepConfig.warn { message ?: "Prince message event without message?" }
                MessageType.ERR -> stepConfig.error { message ?: "Prince message event without message?" }
                else -> stepConfig.info { message ?: "Prince message event without message?" }
            }
        }

        override fun onDataMessage(name: String?, value: String?) {
            stepConfig.debug { "Prince data message: ${name} = ${value}" }
        }
    }
}