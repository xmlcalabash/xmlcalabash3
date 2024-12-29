package com.xmlcalabash.pagedmedia.weasyprint

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssWeasyprint: CssProcessor {
    companion object {
        val _exePath = QName("exePath")
        val _mediaType = QName("media-type")
        val _baseUrl = QName("base-url")
        val _attachment = QName("attachment")
        val _pdfIdentifier = QName("pdf-identifier")
        val _pdfVariant = QName("pdf-variant")
        val _pdfVersion = QName("pdf-version")
        val _pdfForms = QName("pdf-forms")
        val _uncompressedPdf = QName("uncompressed-pdf")
        val _customMetadata = QName("custom-metadata")
        val _presentationalHints = QName("presentational-hints")
        val _optimizeImages = QName("optimize-images")
        val _jpegQuality = QName("jpeg-quality")
        val _fullFonts = QName("full-fonts")
        val _hinting = QName("hinting")
        val _cacheFolder = QName("cache-folder")
        val _dpi = QName("dpi")
        val _verbose = QName("verbose")
        val _debug = QName("debug")
        val _quiet = QName("quiet")
        val _timeout = QName("timeout")

        val booleanOptions = listOf(_customMetadata, _debug, _fullFonts, _hinting, _optimizeImages, _pdfForms,
            _presentationalHints, _quiet, _uncompressedPdf, _verbose)

        val stringOptions = listOf(_exePath, _baseUrl, _attachment, _cacheFolder, _dpi, _jpegQuality, _mediaType,
            _pdfIdentifier, _pdfVariant, _pdfVersion, _timeout)

        val defaultBooleanOptions = mutableMapOf<QName,Boolean>()
        val defaultStringOptions = mutableMapOf<QName,String>()

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != WeasyprintManager.weasyprintCssFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported Weasyprint property: ${key}")
                }
            }

            if (defaultStringOptions[_exePath] == null) {
                val prop = System.getProperty("com.xmlcalabash.css.weasyprint.exepath")
                if (prop != null) {
                    defaultStringOptions[_exePath] = prop
                } else {
                    val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
                        "weasyprint.exe"
                    } else {
                        "weasyprint"
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
    var exePath = ""
    var primarySS: String? = null
    val userSS = mutableListOf<String>()
    val tempFiles = mutableListOf<File>()

    val commandLine = mutableListOf<String>()

    override fun name(): String {
        return "Weasyprint"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options
        primarySS = null
        userSS.clear()
        tempFiles.clear()

        exePath = options[_exePath]?.underlyingValue?.stringValue ?: defaultStringOptions[_exePath] ?: ""
        if (exePath == "") {
            throw stepConfig.exception(XProcError.xdStepFailed("Cannot find Weasyprint executable"))
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
            stepConfig.error { "Ignoring non-text CSS sytlesheet: ${document.baseURI}" }
            return
        }

        val temp = File.createTempFile("xmlcalabash-weasycss", ".css")
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

        commandLine.add(exePath)

        if (primarySS != null) {
            commandLine.add("--stylesheet")
            commandLine.add(primarySS!!)
        }

        for (css in userSS) {
            commandLine.add("--stylesheet")
            commandLine.add(css)
        }

        val baseURI = options[_baseUrl]?.underlyingValue?.stringValue ?: defaultStringOptions[_baseUrl]
        if (baseURI != null) {
            commandLine.add("--base-url")
            commandLine.add(baseURI)
        } else {
            if (document.baseURI != null) {
                commandLine.add("--base-url")
                commandLine.add(document.baseURI.toString())
            }
        }

        for (name in booleanOptions) {
            val value = options[name]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[name]
            if (value != null && value) {
                commandLine.add("--${name.localName}")
            }
        }

        for (name in stringOptions) {
            if (name != _exePath && name != _baseUrl) {
                val value = options[name]?.underlyingValue?.stringValue ?: defaultStringOptions[name]
                if (value != null) {
                    commandLine.add("--${name.localName}")
                    commandLine.add(value)
                }
            }
        }

        val tempXml = File.createTempFile("xmlcalabash-weasycss", ".xml")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        commandLine.add(tempXml.absolutePath)

        stepConfig.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val serializer = XProcSerializer(stepConfig)
        serializer.write(document, tempXml)

        val tempPdf = File.createTempFile("xmlcalabash-weasycss", ".pdf")
        tempPdf.deleteOnExit()
        tempFiles.add(tempPdf)

        commandLine.add(tempPdf.absolutePath)

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        stepConfig.debug { commandLine.joinToString(" ") }

        //println("RUN: ${commandLine.joinToString(" ")}")

        val builder = ProcessBuilder(commandLine)

        val rc = try {
            val process = builder.start()

            val stdoutReader = ProcessOutputReader(process.inputStream, stdout)
            val stderrReader = ProcessOutputReader(process.errorStream, stderr)

            val stdoutThread = Thread(stdoutReader)
            val stderrThread = Thread(stderrReader)

            stdoutThread.start()
            stderrThread.start()

            val localrc = process.waitFor()
            stdoutThread.join()
            stderrThread.join()

            localrc
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw stepConfig.exception(XProcError.xcOsExecFailed())
        }

        if (rc != 0) {
            println(stdout.toString())
            println(stderr.toString())
            throw stepConfig.exception(XProcError.xdStepFailed("Weasyprint failed: ${rc}"))
        }

        val readPdf = FileInputStream(tempPdf)
        val buffer = ByteArray(4096)
        var len = readPdf.read(buffer)
        while (len >= 0) {
            out.write(buffer, 0, len)
            len = readPdf.read(buffer)
        }
        readPdf.close()

        while (tempFiles.isNotEmpty()) {
            val temp = tempFiles.removeAt(0)
            try {
                temp.delete()
            } catch (ex: Exception) {
                // nop
            }
        }
    }

    inner class ProcessOutputReader(val stream: InputStream, val buffer: ByteArrayOutputStream): Runnable {
        val tree = SaxonTreeBuilder(stepConfig)

        override fun run() {
            tree.startDocument(null)
            tree.addStartElement(NsC.result)
            val reader = InputStreamReader(stream)
            val buf = CharArray(4096)
            var len = reader.read(buf)
            while (len >= 0) {
                if (len == 0) {
                    Thread.sleep(250)
                } else {
                    // This is the most efficient way? Really!?
                    for (pos in 0 until len) {
                        buffer.write(buf[pos].code)
                    }
                }
                len = reader.read(buf)
            }
        }
    }
}
