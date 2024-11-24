package com.xmlcalabash.pagedmedia.prince

import com.princexml.Prince
import com.princexml.PrinceEvents
import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.config.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssPrince: CssProcessor {
    companion object {
        private val _exePath = QName("exePath")
        private val _baseURL = QName("baseURL")
        private val _compress = QName("compress")
        private val _debug = QName("debug")
        private val _embedFonts = QName("embedFonts")
        private val _encrypt = QName("encrypt")
        private val _keyBits = QName("keyBits")
        private val _userPassword = QName("userPassword")
        private val _ownerPassword = QName("ownerPassword")
        private val _disallowPrint = QName("disallowPrint")
        private val _disallowModify = QName("disallowModify")
        private val _disallowCopy = QName("disallowCopy")
        private val _disallowAnnotate = QName("disallowAnnotate")
        private val _fileRoot = QName("fileRoot")
        private val _html = QName("html")
        private val _httpPassword = QName("httpPassword")
        private val _httpUsername = QName("httpUsername")
        private val _httpProxy = QName("httpProxy")
        private val _inputType = QName("inputType")
        private val _javascript = QName("javascript")
        private val _log = QName("log")
        private val _network = QName("network")
        private val _subsetFonts = QName("subsetFonts")
        private val _verbose = QName("verbose")
        private val _XInclude = QName("XInclude")
        private val _scripts = QName("scripts")
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

        val exePath = if (options.containsKey(_exePath)) {
            options[_exePath]!!.underlyingValue.stringValue
        } else {
            val prop = System.getProperty("com.xmlcalabash.css.prince.exepath")
            if (prop != null) {
                prop
            } else {
                val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
                    "prince.exe"
                } else {
                    "prince"
                }

                var found = ""
                for (path in System.getenv("PATH").split(File.pathSeparator)) {
                    val exe = Paths.get(path, exeName).toFile()
                    if (exe.exists() && exe.canExecute()) {
                        found = exe.absolutePath
                        break
                    }
                }

                found
            }
        }

        if (exePath == "") {
            throw XProcError.xdStepFailed("Cannot find Prince XML executable").exception()
        }

        prince = Prince(exePath, PrinceMessages())

        stringOption(_baseURL) { prince.setBaseURL(it) }
        stringOption(_fileRoot) { prince.setFileRoot(it) }
        stringOption(_httpPassword) { prince.setHttpPassword(it) }
        stringOption(_httpUsername) { prince.setHttpUsername(it) }
        stringOption(_httpProxy) { prince.setHttpProxy(it) }
        stringOption(_inputType) { prince.setInputType(it) }
        stringOption(_log) { prince.setLog(it) }

        booleanOption(_compress) { prince.setCompress(it) }
        booleanOption(_debug) { prince.setDebug(it) }
        booleanOption(_embedFonts) { prince.setEmbedFonts(it) }
        booleanOption(_html) { prince.setHTML(it) }
        booleanOption(_javascript) { prince.setJavaScript(it) }
        booleanOption(_network) { prince.setNetwork(it) }
        booleanOption(_subsetFonts) { prince.setSubsetFonts(it) }
        booleanOption(_verbose) { prince.setVerbose(it) }
        booleanOption(_XInclude) { prince.setXInclude(it) }

        booleanOption(_encrypt) { prince.setEncrypt(it) }
        if (options.containsKey(_keyBits)) {
            val keyBits = options[_keyBits]!!.underlyingValue.stringValue.toInt()
            var up = ""
            var op = ""
            var dp = false
            var dm = false
            var dc = false
            var da = false

            stringOption(_userPassword) { up = it }
            stringOption(_ownerPassword) { op = it }
            booleanOption(_disallowPrint) { dp = it }
            booleanOption(_disallowModify) { dm = it }
            booleanOption(_disallowCopy) { dc = it }
            booleanOption(_disallowAnnotate) { da = it }

            prince.setEncryptInfo(keyBits, up, op, dp, dm, dc, da)
        }

        if (options.containsKey(_scripts)) {
            for (script in options[_scripts]!!.underlyingValue.stringValue.split("\\s+".toRegex())) {
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

    private fun stringOption(name: QName, setter: (String) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.stringValue
            setter(value)
        }
    }

    private fun booleanOption(name: QName, setter: (Boolean) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.effectiveBooleanValue()
            setter(value)
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