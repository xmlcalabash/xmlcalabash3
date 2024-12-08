package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.runtime.XProcStepConfiguration
import jp.co.antenna.XfoJavaCtl.XfoObj
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class FoAH(): AbstractAH(), FoProcessor {
    override fun name(): String {
        return "Antenna House"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options

        ah = XfoObj()
        ah.setFormatterType(XfoObj.S_FORMATTERTYPE_XSLFO)
        ahInitialize()
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        val outputFormat = formatMap[contentType]
            ?: throw XProcError.xdInvalidContentType(contentType.toString()).exception()

        val temp = File.createTempFile("xmlcalabash-ahfo", ".xml")
        temp.deleteOnExit()

        logger.debug { "xsl-formatter source: ${temp.absolutePath}" }

        // AH won't parse HTML
        val sourceContentType = document.contentType ?: MediaType.OCTET_STREAM
        val overrideContentType = if (sourceContentType == MediaType.HTML) {
            MediaType.XHTML
        } else {
            null
        }

        val serializer = XProcSerializer(stepConfig)
        val fos = FileOutputStream(temp)
        serializer.write(document, fos, overrideContentType)
        fos.close()

        val fis = FileInputStream(temp)
        ah.render(fis, out, outputFormat)
        ah.releaseObjectEx()

        try {
            temp.delete()
        } catch (ex: Exception) {
            // nop
        }
    }
}