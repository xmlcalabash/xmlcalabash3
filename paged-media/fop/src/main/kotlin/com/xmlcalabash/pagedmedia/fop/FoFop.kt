package com.xmlcalabash.pagedmedia.fop

import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.fop.apps.FopFactory
import org.apache.fop.apps.FopFactoryBuilder
import org.apache.fop.configuration.DefaultConfigurationBuilder
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.text.DateFormat
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXSource

class FoFop(): FoProcessor {
    companion object {
        private val supportedContentTypes = listOf(MediaType.PDF, MediaType.parse("application/postscript"),
            MediaType.parse("application/x-afp"), MediaType.parse("application/rtf"),
            MediaType.TEXT)

        private val _UserConfig = QName("UserConfig")
        private val _StrictFOValidation = QName("StrictFOValidation")
        private val _BreakIndentInheritanceOnReferenceAreaBoundary = QName("BreakIndentInheritanceOnReferenceAreaBoundary")
        private val _SourceResolution = QName("SourceResolution")
        private val _Base14KerningEnabled = QName("Base14KerningEnabled")
        private val _PageHeight = QName("PageHeight")
        private val _PageWidth = QName("PageWidth")
        private val _TargetResolution = QName("TargetResolution")
        private val _StrictUserConfigValidation = QName("StrictUserConfigValidation")
        private val _StrictValidation = QName("StrictValidation")
        private val _UseCache = QName("UseCache")

        private val _Accessibility = QName("Accessibility")
        private val _Author = QName("Author")
        private val _ConserveMemoryPolicy = QName("ConserveMemoryPolicy")
        private val _CreationDate = QName("CreationDate")
        private val _Creator = QName("Creator")
        private val _Keywords = QName("Keywords")
        private val _LocatorEnabled = QName("LocatorEnabled")
        private val _Producer = QName("Producer")
        private val _Subject = QName("Subject")
        private val _Title = QName("Title")
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var options: Map<QName, XdmValue>
    lateinit var fopFactory: FopFactory

    override fun name(): String {
        return "Apache FOP"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options

        // Only FOP 2.x is supported

        val fopBuilder = if (options.containsKey(_UserConfig)) {
            val opt = options[_UserConfig]!!.underlyingValue.stringValue
            val cfgBuilder = DefaultConfigurationBuilder()
            val cfg = cfgBuilder.buildFromFile(File(opt))
            FopFactoryBuilder(baseURI).setConfiguration(cfg)
        } else {
            FopFactoryBuilder(baseURI)
        }

        booleanOption(_StrictFOValidation) { fopBuilder.setStrictFOValidation(it) }
        booleanOption(_BreakIndentInheritanceOnReferenceAreaBoundary) { fopBuilder.setBreakIndentInheritanceOnReferenceAreaBoundary(it) }
        booleanOption(_Base14KerningEnabled) { fopBuilder.fontManager.isBase14KerningEnabled = it }
        booleanOption(_StrictUserConfigValidation) { fopBuilder.setStrictUserConfigValidation(it) }
        booleanOption(_StrictValidation) { fopBuilder.setStrictUserConfigValidation(it) }
        floatOption(_SourceResolution) { fopBuilder.setSourceResolution(it) }
        floatOption(_TargetResolution) { fopBuilder.setTargetResolution(it) }
        stringOption(_PageHeight) { fopBuilder.setPageHeight(it) }
        stringOption(_PageWidth) { fopBuilder.setPageWidth(it) }

        if (options.containsKey(_UseCache)) {
            if (!options[_UseCache]!!.underlyingValue.effectiveBooleanValue()) {
                fopBuilder.fontManager.disableFontCache()
            }
        }

        fopFactory = fopBuilder.build()
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (!supportedContentTypes.contains(contentType)) {
            throw XProcError.xcUnsupportedContentType(contentType).exception()
        }

        val fodoc = S9Api.xdmToInputSource(stepConfig, document)
        val source = SAXSource(fodoc)

        val userAgent = fopFactory.newFOUserAgent()

        booleanOption(_Accessibility) { userAgent.setAccessibility(it) }
        booleanOption(_ConserveMemoryPolicy) { userAgent.setConserveMemoryPolicy(it) }
        booleanOption(_LocatorEnabled) { userAgent.isLocatorEnabled = it }
        stringOption(_Author) { userAgent.author = it }
        stringOption(_Creator) { userAgent.creator = it }
        stringOption(_Keywords) { userAgent.keywords = it }
        stringOption(_Producer) { userAgent.producer = it }
        stringOption(_Subject) { userAgent.subject = it }
        stringOption(_Title) { userAgent.title = it }
        floatOption(_TargetResolution) { userAgent.targetResolution = it }

        if (options.containsKey(_CreationDate)) {
            val df = DateFormat.getDateInstance()
            val d = df.parse(options[_CreationDate]!!.underlyingValue.stringValue)
            userAgent.setCreationDate(d)
        }

        val fop = userAgent.newFop(contentType.toString(), out)
        val defHandler = fop.defaultHandler

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.transform(source, SAXResult(defHandler))
    }

    protected fun stringOption(name: QName, setter: (String) -> Unit) {
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

    protected fun floatOption(name: QName, setter: (Float) -> Unit) {
        if (options.containsKey(name)) {
            val value = options[name]!!.underlyingValue.stringValue.toFloat()
            setter(value)
        }
    }
}