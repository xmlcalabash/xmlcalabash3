package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.config.XProcStepConfiguration
import jp.co.antenna.XfoJavaCtl.XfoObj
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI

class CssAH(): AbstractAH(), CssProcessor {
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
        ahInitialize()
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

        val temp = File.createTempFile("xmlcalabash-ahcss", ".css")
        temp.deleteOnExit()
        tempFiles.add(temp)

        logger.debug { "css-formatter css: ${temp.absolutePath}" }

        val cssout = PrintStream(temp)
        cssout.print(document.value.underlyingValue.stringValue)
        cssout.close()

        addStylesheet(temp.toURI())
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        val outputFormat = formatMap[contentType]
            ?: throw XProcError.xcUnsupportedContentType(contentType).exception()

        if (primarySS == null) {
            logger.error { "No CSS stylesheet provided for p:css-formatter" }
        } else {
            ah.setStylesheetURI(primarySS)
        }

        for (css in userSS) {
            ah.addUserStylesheetURI(css)
        }

        val tempXml = File.createTempFile("xmlcalabash-ahcss", ".xml")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        logger.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val serializer = XProcSerializer(stepConfig)
        val fos = FileOutputStream(tempXml)

        // AH won't parse HTML
        val sourceContentType = document.contentType ?: MediaType.OCTET_STREAM
        val overrideContentType = if (sourceContentType == MediaType.HTML) {
            MediaType.XHTML
        } else {
            null
        }

        serializer.write(document, fos, overrideContentType)
        fos.close()

        val fis = FileInputStream(tempXml)

        try {
            ah.render(fis, out, outputFormat)
        } catch (ex: Exception) {
            throw XProcError.xdStepFailed(ex.message ?: "AH failed").exception(ex)

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