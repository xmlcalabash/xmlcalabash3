package com.xmlcalabash.pagedmedia.prince

import com.princexml.Prince
import com.princexml.PrinceEvents
import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssPrince: CssProcessor {
    companion object {
        val _exePath = QName("exePath")
        val _baseURL = QName("baseURL")
        val _compress = QName("compress")
        val _debug = QName("debug")
        val _embedFonts = QName("embedFonts")
        val _encrypt = QName("encrypt")
        val _keyBits = QName("keyBits")
        val _userPassword = QName("userPassword")
        val _ownerPassword = QName("ownerPassword")
        val _disallowPrint = QName("disallowPrint")
        val _disallowModify = QName("disallowModify")
        val _disallowCopy = QName("disallowCopy")
        val _disallowAnnotate = QName("disallowAnnotate")
        val _fileRoot = QName("fileRoot")
        val _html = QName("html")
        val _httpPassword = QName("httpPassword")
        val _httpUsername = QName("httpUsername")
        val _httpProxy = QName("httpProxy")
        val _inputType = QName("inputType")
        val _javascript = QName("javascript")
        val _log = QName("log")
        val _network = QName("network")
        val _subsetFonts = QName("subsetFonts")
        val _verbose = QName("verbose")
        val _xinclude = QName("xinclude")
        val _scripts = QName("scripts")

        val stringOptions = listOf(_exePath, _baseURL, _keyBits, _userPassword, _ownerPassword,
            _fileRoot, _httpPassword, _httpUsername, _httpProxy, _inputType, _log, _scripts)
        val booleanOptions = listOf(_compress, _debug, _encrypt, _embedFonts, _html, _javascript, _network,
            _subsetFonts, _verbose, _xinclude, _disallowAnnotate, _disallowCopy, _disallowModify, _disallowPrint)

        val defaultBooleanOptions = mutableMapOf<QName,Boolean>()
        val defaultStringOptions = mutableMapOf<QName,String>()

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
            throw XProcError.xdStepFailed("Cannot find Prince XML executable").exception()
        }

        prince = Prince(exePath, PrinceMessages())

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _baseURL -> prince.setBaseURL(value)
                    _fileRoot -> prince.setFileRoot(value)
                    _httpPassword -> prince.setHttpPassword(value)
                    _httpUsername -> prince.setHttpUsername(value)
                    _httpProxy -> prince.setHttpProxy(value)
                    _inputType -> prince.setInputType(value)
                    _log -> prince.setLog(value)
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _compress -> prince.setCompress(value)
                    _debug -> prince.setDebug(value)
                    _embedFonts -> prince.setEmbedFonts(value)
                    _html -> prince.setHTML(value)
                    _javascript -> prince.setJavaScript(value)
                    _network -> prince.setNetwork(value)
                    _subsetFonts -> prince.setSubsetFonts(value)
                    _verbose -> prince.setVerbose(value)
                    _xinclude -> prince.setXInclude(value)
                    _encrypt -> prince.setEncrypt(value)
                    else -> Unit
                }
            }
        }

        val keyBitStr = options[_keyBits]?.underlyingValue?.stringValue ?: defaultStringOptions[_keyBits]
        if (keyBitStr != null) {
            val keyBits = keyBitStr.toInt()
            var up = options[_userPassword]?.underlyingValue?.stringValue ?: defaultStringOptions[_userPassword] ?: ""
            var op = options[_ownerPassword]?.underlyingValue?.stringValue ?: defaultStringOptions[_ownerPassword] ?: ""
            var dp = options[_disallowPrint]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[_disallowPrint] ?: false
            var dm = options[_disallowModify]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[_disallowModify] ?: false
            var dc = options[_disallowCopy]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[_disallowCopy] ?: false
            var da = options[_disallowAnnotate]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[_disallowAnnotate] ?: false
            prince.setEncryptInfo(keyBits, up, op, dp, dm, dc, da)
        }

        val scripts = options[_scripts]?.underlyingValue?.stringValue ?: defaultStringOptions[_scripts]
        if (scripts != null) {
            for (script in scripts.split("\\s+".toRegex())) {
                prince.addScript(script)
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
        if (document.contentType == null || !document.contentType!!.textContentType()) {
            logger.error("Ignoring non-text CSS sytlesheet: ${document.baseURI}")
            return
        }

        val temp = File.createTempFile("xmlcalabash-princecss", ".css")
        temp.deleteOnExit()
        tempFiles.add(temp)

        logger.debug { "css-formatter css: ${temp.absolutePath}" }

        val cssout = PrintStream(temp)
        cssout.print(document.value.underlyingValue.stringValue)
        cssout.close()

        addStylesheet(temp.toURI())
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (contentType != MediaType.PDF) {
            throw XProcError.xcUnsupportedContentType(contentType).exception()
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

        logger.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val serializer = XProcSerializer(stepConfig)
        val fos = FileOutputStream(tempXml)
        serializer.write(document, fos)

        val fis = FileInputStream(tempXml)
        prince.convert(fis, out)

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
        override fun onMessage(msgType: String?, msgLoc: String?, message: String?) {
            when (msgType) {
                "inf" -> logger.info { message }
                "wrn" -> logger.warn { message }
                else -> logger.error { message }
            }
        }
    }
}