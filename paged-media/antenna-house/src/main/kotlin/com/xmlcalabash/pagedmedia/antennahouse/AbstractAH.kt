package com.xmlcalabash.pagedmedia.antennahouse

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import jp.co.antenna.XfoJavaCtl.MessageListener
import jp.co.antenna.XfoJavaCtl.XfoFormatPageListener
import jp.co.antenna.XfoJavaCtl.XfoObj
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger

abstract class AbstractAH() {
    companion object {
        @JvmStatic
        protected val formatMap = mapOf(
            MediaType.PDF to "@PDF",
            MediaType.parse("application/postscript") to "@PS",
            MediaType.parse("application/vnd.inx") to "@INX",
            MediaType.parse("application/vnd.mif") to "@MIF",
            MediaType.parse("image/svg+xml") to "@SVG",
            MediaType.TEXT to "@TEXT"
        )

        @JvmStatic
        protected val _OptionFileURI = QName("OptionFileURI")
        @JvmStatic
        protected val _ExitLevel = QName("ExitLevel")
        @JvmStatic
        protected val _EmbedAllFontsEx = QName("EmbedAllFontsEx")
        @JvmStatic
        protected val _ImageCompression = QName("ImageCompression")
        @JvmStatic
        protected val _NoAccessibility = QName("NoAccessibility")
        @JvmStatic
        protected val _NoAddingOrChangingComments = QName("NoAddingOrChangingComments")
        @JvmStatic
        protected val _NoAssembleDoc = QName("NoAssembleDoc")
        @JvmStatic
        protected val _NoChanging = QName("NoChanging")
        @JvmStatic
        protected val _NoContentCopying = QName("NoContentCopying")
        @JvmStatic
        protected val _NoFillForm = QName("NoFillForm")
        @JvmStatic
        protected val _NoPrinting = QName("NoPrinting")
        @JvmStatic
        protected val _OwnersPassword = QName("OwnersPassword")
        @JvmStatic
        protected val _TwoPassFormatting = QName("TwoPassFormatting")
    }

    protected lateinit var stepConfig: XProcStepConfiguration
    protected lateinit var options: Map<QName, XdmValue>
    protected lateinit var ah: XfoObj

    protected fun ahInitialize() {
        ah.setMessageListener(FoMessages())

        if (options.containsKey(_EmbedAllFontsEx)) {
            val embed = options[_EmbedAllFontsEx]!!.underlyingValue.stringValue
            when (embed) {
                "part" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_PART)
                "base14" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_BASE14)
                "all" -> ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_ALL)
                else -> logger.error("Ignoring unknown EmbedAllFontsEx option: ${embed}")
            }
        }

        stringOption(_OptionFileURI) { ah.setOptionFileURI(it) }
        stringOption(_OwnersPassword) { ah.setPdfOwnerPassword(it) }

        intOption(_ExitLevel) { ah.setExitLevel(it) }
        intOption(_ImageCompression) { ah.setPdfImageCompression(it) }

        booleanOption(_NoAccessibility) { ah.setPdfNoAccessibility(it) }
        booleanOption(_NoAddingOrChangingComments) { ah.setPdfNoAddingOrChangingComments(it) }
        booleanOption(_NoAssembleDoc) { ah.setPdfNoAssembleDoc(it) }
        booleanOption(_NoChanging) { ah.setPdfNoChanging(it) }
        booleanOption(_NoContentCopying) { ah.setPdfNoContentCopying(it) }
        booleanOption(_NoFillForm) { ah.setPdfNoFillForm(it) }
        booleanOption(_NoPrinting) { ah.setPdfNoPrinting(it) }
        booleanOption(_TwoPassFormatting) { ah.setTwoPassFormatting(it) }
    }

    fun stringOption(name: QName, setter: (String) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.stringValue
            setter(value)
        }
    }

    protected fun booleanOption(name: QName, setter: (Boolean) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.effectiveBooleanValue()
            setter(value)
        }
    }

    protected fun intOption(name: QName, setter: (Int) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.stringValue.toInt()
            setter(value)
        }
    }

    inner class FoMessages(): MessageListener, XfoFormatPageListener {
        override fun onMessage(errorLevel: Int, errorCode: Int, errorMessage: String?) {
            if (errorMessage == null) {
                return
            }

            when (errorLevel) {
                1 -> logger.info(errorMessage)
                2 -> logger.warn(errorMessage)
                else -> logger.error(errorMessage)
            }
        }

        override fun onFormatPage(pageNo: Int) {
            logger.debug("Formatted PDF page ${pageNo}")
        }
    }
}