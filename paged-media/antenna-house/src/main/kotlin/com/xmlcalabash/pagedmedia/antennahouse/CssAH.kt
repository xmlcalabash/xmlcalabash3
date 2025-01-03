package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentConverter
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import jp.co.antenna.XfoJavaCtl.XfoObj
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI

class CssAH(): AbstractAH(), CssProcessor {
    companion object {
        protected val defaultStringOptions = mutableMapOf<QName, String>()
        protected val defaultIntOptions = mutableMapOf<QName, Int>()
        protected val defaultBooleanOptions = mutableMapOf<QName, Boolean>()
        protected var defaultEmbedAllFonts: String? = null

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != AhManager.ahCssFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key == _EmbedAllFontsEx) {
                    if (value == "part" || value == "base14" || value == "all") {
                        defaultEmbedAllFonts = value
                    } else {
                        logger.warn("Ignoring unknown Antenna House CSS EmbedAllFontsEx option: ${value}")
                    }
                } else if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in intOptions) {
                    defaultIntOptions[key] = value.toInt()
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported Antenna House CSS property: ${key}")
                }
            }
        }
    }

    var primarySS: String? = null
    val userSS = mutableListOf<String>()
    val tempFiles = mutableListOf<File>()

    override fun name(): String {
        return "Antenna House"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options
        primarySS = null
        userSS.clear()
        tempFiles.clear()

        ah = XfoObj()
        ah.setFormatterType(XfoObj.S_FORMATTERTYPE_XMLCSS)
        ahInitialize(defaultStringOptions, defaultIntOptions, defaultBooleanOptions, defaultEmbedAllFonts)
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

        val temp = File.createTempFile("xmlcalabash-ahcss", ".css")
        temp.deleteOnExit()
        tempFiles.add(temp)

        stepConfig.debug { "css-formatter css: ${temp.absolutePath}" }

        val cssout = PrintStream(temp)
        cssout.print(document.value.underlyingValue.stringValue)
        cssout.close()

        addStylesheet(temp.toURI())
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        val outputFormat = formatMap[contentType]
            ?: throw stepConfig.exception(XProcError.xcUnsupportedContentType(contentType))

        if (primarySS == null) {
            stepConfig.error { "No CSS stylesheet provided for p:css-formatter" }
        } else {
            ah.setStylesheetURI(primarySS)
        }

        for (css in userSS) {
            ah.addUserStylesheetURI(css)
        }

        val tempXml = File.createTempFile("xmlcalabash-ahcss", ".xml")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        stepConfig.debug { "css-formatter source: ${tempXml.absolutePath}" }

        // AH won't parse HTML
        val sourceContentType = document.contentType ?: MediaType.OCTET_STREAM
        val ahDoc = if (sourceContentType == MediaType.HTML) {
            DocumentConverter(stepConfig, document, MediaType.XHTML).convert()
        } else {
            document
        }

        val fos = FileOutputStream(tempXml)
        DocumentWriter(ahDoc, fos).write()
        fos.close()
        val fis = FileInputStream(tempXml)

        try {
            ah.render(fis, out, outputFormat)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "AH failed"), ex)

        } finally {
            ah.releaseObjectEx()
        }

        while (tempFiles.isNotEmpty()) {
            val temp = tempFiles.removeAt(0)
            try {
                temp.delete()
            } catch (ex: Exception) {
                // nop
            }
        }
    }
}