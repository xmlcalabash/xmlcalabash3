package com.xmlcalabash.pagedmedia.weasyprint

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssWeasyprint: CssProcessor {
    companion object {
        private val _exePath = QName("exePath")
        private val _mediaType = QName("media-type")
        private val _baseUrl = QName("base-url")
        private val _attachment = QName("attachment")
        private val _pdfIdentifier = QName("pdf-identifier")
        private val _pdfVariant = QName("pdf-variant")
        private val _pdfVersion = QName("pdf-version")
        private val _pdfForms = QName("pdf-forms")
        private val _uncompressedPdf = QName("uncompressed-pdf")
        private val _customMetadata = QName("custom-metadata")
        private val _presentationalHints = QName("presentational-hints")
        private val _optimizeImages = QName("optimize-images")
        private val _jpegQuality = QName("jpeg-quality")
        private val _fullFonts = QName("full-fonts")
        private val _hinting = QName("hinting")
        private val _cacheFolder = QName("cache-folder")
        private val _dpi = QName("dpi")
        private val _verbose = QName("verbose")
        private val _debug = QName("debug")
        private val _quiet = QName("quiet")
        private val _timeout = QName("timeout")
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

        exePath = if (options.containsKey(_exePath)) {
            options[_exePath]!!.underlyingValue.stringValue
        } else {
            val prop = System.getProperty("com.xmlcalabash.css.weasyprint.exepath")
            if (prop != null) {
                prop
            } else {
                val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
                    "weasyprint.exe"
                } else {
                    "weasyprint"
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
            throw XProcError.xdStepFailed("Cannot find Weasyprint executable").exception()
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

        val temp = File.createTempFile("xmlcalabash-weasycss", ".css")
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

        commandLine.add(exePath)

        if (primarySS != null) {
            commandLine.add("--stylesheet")
            commandLine.add(primarySS!!)
        }

        for (css in userSS) {
            commandLine.add("--stylesheet")
            commandLine.add(css)
        }

        if (options.containsKey(_baseUrl)) {
            stringOption(_baseUrl)
        } else {
            if (document.baseURI != null) {
                commandLine.add("--base-url")
                commandLine.add(document.baseURI.toString())
            }
        }

        stringOption(_mediaType)
        stringOption(_attachment)
        stringOption(_pdfIdentifier)
        stringOption(_pdfVariant)
        stringOption(_pdfVariant)
        booleanOption(_pdfForms)
        booleanOption(_uncompressedPdf)
        booleanOption(_customMetadata)
        booleanOption(_presentationalHints)
        booleanOption(_optimizeImages)
        stringOption(_jpegQuality)
        booleanOption(_fullFonts)
        booleanOption(_hinting)
        stringOption(_cacheFolder)
        stringOption(_dpi)
        booleanOption(_verbose)
        booleanOption(_quiet)
        booleanOption(_debug)
        stringOption(_timeout)

        val tempXml = File.createTempFile("xmlcalabash-weasycss", ".xml")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        commandLine.add(tempXml.absolutePath)

        logger.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val serializer = XProcSerializer(stepConfig)
        val fos = FileOutputStream(tempXml)
        serializer.write(document, fos)

        val tempPdf = File.createTempFile("xmlcalabash-weasycss", ".pdf")
        tempPdf.deleteOnExit()
        tempFiles.add(tempPdf)

        commandLine.add(tempPdf.absolutePath)

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        logger.debug { commandLine.joinToString(" ") }

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
            throw XProcError.xcOsExecFailed().exception()
        }

        if (rc != 0) {
            println(stdout.toString())
            println(stderr.toString())
            throw XProcError.xdStepFailed("Weasyprint failed: ${rc}").exception()
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

    protected fun stringOption(name: QName) {
        if (options.containsKey(name)) {
            val valueList = options[name]!!.underlyingValue
            for (value in valueList.asIterable()) {
                commandLine.add("--${name.localName}")
                commandLine.add(value.toString())
            }
        }
    }

    protected fun booleanOption(name: QName) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.effectiveBooleanValue()
            if (value) {
                commandLine.add("--${name.localName}")
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
