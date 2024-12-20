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
    companion object {
        protected val defaultStringOptions = mutableMapOf<QName, String>()
        protected val defaultIntOptions = mutableMapOf<QName, Int>()
        protected val defaultBooleanOptions = mutableMapOf<QName, Boolean>()
        protected var defaultEmbedAllFonts: String? = null

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != AhManager.ahXslFormatter) {
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
                    logger.warn("Unsupported Antenna House XSL FO property: ${key}")
                }
            }
        }
    }

    override fun name(): String {
        return "Antenna House"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options

        ah = XfoObj()
        ah.setFormatterType(XfoObj.S_FORMATTERTYPE_XSLFO)

        ahInitialize(defaultStringOptions, defaultIntOptions, defaultBooleanOptions, defaultEmbedAllFonts)
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        val outputFormat = formatMap[contentType]
            ?: throw stepConfig.exception(XProcError.xdInvalidContentType(contentType.toString()))

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