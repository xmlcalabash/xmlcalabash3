package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import jp.co.antenna.XfoJavaCtl.MessageListener
import jp.co.antenna.XfoJavaCtl.XfoFormatPageListener
import jp.co.antenna.XfoJavaCtl.XfoObj
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger

abstract class AbstractAH() {
    companion object {
        val formatMap = mapOf(
            MediaType.PDF to "@PDF",
            MediaType.parse("application/postscript") to "@PS",
            MediaType.parse("application/vnd.inx") to "@INX",
            MediaType.parse("application/vnd.mif") to "@MIF",
            MediaType.parse("image/svg+xml") to "@SVG",
            MediaType.TEXT to "@TEXT"
        )

        val _OptionFileURI = QName("OptionFileURI")
        val _ExitLevel = QName("ExitLevel")
        val _EmbedAllFontsEx = QName("EmbedAllFontsEx")
        val _ImageCompression = QName("ImageCompression")
        val _NoAccessibility = QName("NoAccessibility")
        val _NoAddingOrChangingComments = QName("NoAddingOrChangingComments")
        val _NoAssembleDoc = QName("NoAssembleDoc")
        val _NoChanging = QName("NoChanging")
        val _NoContentCopying = QName("NoContentCopying")
        val _NoFillForm = QName("NoFillForm")
        val _NoPrinting = QName("NoPrinting")
        val _OwnersPassword = QName("OwnersPassword")
        val _TwoPassFormatting = QName("TwoPassFormatting")

        val stringOptions = listOf(_OptionFileURI, _OwnersPassword)
        val intOptions = listOf(_ExitLevel, _ImageCompression)
        val booleanOptions = listOf(_NoAccessibility, _NoAddingOrChangingComments, _NoAssembleDoc,
            _NoChanging, _NoContentCopying, _NoFillForm, _NoPrinting, _TwoPassFormatting)
    }

    protected lateinit var stepConfig: XProcStepConfiguration
    protected lateinit var options: Map<QName, XdmValue>
    protected lateinit var ah: XfoObj

    protected fun ahInitialize(defaultStringOptions: Map<QName, String>,
                               defaultIntOptions: Map<QName, Int>,
                               defaultBooleanOptions: Map<QName, Boolean>,
                               defaultEmbedAllFonts: String?) {
        ah.setMessageListener(AhMessages())

        for (key in options.keys) {
            if (key != _EmbedAllFontsEx && key !in stringOptions && key !in intOptions && key !in booleanOptions) {
                stepConfig.warn { "Unsupported Antenna House XSL FO property: ${key}" }
            }
        }

        val embed = options[_EmbedAllFontsEx] ?: defaultEmbedAllFonts
        if (embed != null) {
            when (embed) {
                "part" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_PART)
                "base14" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_BASE14)
                "all" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_ALL)
                else -> stepConfig.warn { "Ignoring unknown Antennah House CSS EmbedAllFontsEx option: ${embed}" }
            }
        }

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _OptionFileURI -> ah.setOptionFileURI(value)
                    _OwnersPassword -> ah.setPdfOwnerPassword(value)
                    else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected string option: ${key}"))
                }
            }
        }

        for (key in intOptions) {
            val value = options[key]?.underlyingValue?.stringValue?.toInt() ?: defaultIntOptions[key]
            if (value != null) {
                when (key) {
                    _ExitLevel -> ah.setExitLevel(value)
                    _ImageCompression -> ah.setPdfImageCompression(value)
                    else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected integer option: ${key}"))
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _NoAccessibility -> ah.setPdfNoAccessibility(value)
                    _NoAddingOrChangingComments -> ah.setPdfNoAddingOrChangingComments(value)
                    _NoAssembleDoc -> ah.setPdfNoAssembleDoc(value)
                    _NoChanging -> ah.setPdfNoChanging(value)
                    _NoContentCopying -> ah.setPdfNoContentCopying(value)
                    _NoFillForm -> ah.setPdfNoFillForm(value)
                    _NoPrinting -> ah.setPdfNoPrinting(value)
                    _TwoPassFormatting -> ah.setTwoPassFormatting(value)
                    else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected boolean option: ${key}"))
                }
            }
        }
    }

    inner class AhMessages(): MessageListener, XfoFormatPageListener {
        override fun onMessage(errorLevel: Int, errorCode: Int, errorMessage: String?) {
            if (errorMessage == null) {
                return
            }

            when (errorLevel) {
                1 -> stepConfig.info { errorMessage }
                2 -> stepConfig.warn { errorMessage }
                else -> stepConfig.error { errorMessage }
            }
        }

        override fun onFormatPage(pageNo: Int) {
            stepConfig.debug { "Formatted PDF page ${pageNo}" }
        }
    }
}