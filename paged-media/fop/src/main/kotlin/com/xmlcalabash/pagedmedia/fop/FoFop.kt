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
import org.apache.logging.log4j.kotlin.logger
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

        private val stringOptions = listOf(_UserConfig, _PageHeight, _PageWidth,
            _Author, _Creator, _Keywords, _Producer, _Subject, _Title, _CreationDate)
        private val booleanOptions = listOf(_StrictFOValidation, _BreakIndentInheritanceOnReferenceAreaBoundary,
            _Base14KerningEnabled, _StrictUserConfigValidation, _StrictValidation, _UseCache, _Accessibility,
            _ConserveMemoryPolicy, _LocatorEnabled)
        private val floatOptions = listOf(_SourceResolution, _TargetResolution)

        private val defaultStringOptions = mutableMapOf<QName, String>()
        private val defaultBooleanOptions = mutableMapOf<QName, Boolean>()
        private val defaultFloatOptions = mutableMapOf<QName, Float>()

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != FopManager.fopXslFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in floatOptions) {
                    defaultFloatOptions[key] = value.toFloat()
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported FOP property: ${key}")
                }
            }
        }
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
        val userConfig = options[_UserConfig]?.underlyingValue?.stringValue ?: defaultStringOptions[_UserConfig]
        val fopBuilder = if (userConfig != null) {
            val cfgBuilder = DefaultConfigurationBuilder()
            val cfg = cfgBuilder.buildFromFile(File(userConfig))
            FopFactoryBuilder(baseURI).setConfiguration(cfg)
        } else {
            FopFactoryBuilder(baseURI)
        }

        for (key in options.keys) {
            if (key != _CreationDate && key !in stringOptions && key !in floatOptions && key !in booleanOptions) {
                logger.warn("Unsupported FOP property: ${key}")
            }
        }

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _PageHeight -> fopBuilder.setPageHeight(value)
                    _PageWidth -> fopBuilder.setPageWidth(value)
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _StrictFOValidation -> fopBuilder.setStrictFOValidation(value)
                    _BreakIndentInheritanceOnReferenceAreaBoundary -> fopBuilder.setBreakIndentInheritanceOnReferenceAreaBoundary(value)
                    _Base14KerningEnabled -> fopBuilder.fontManager.isBase14KerningEnabled = value
                    _StrictUserConfigValidation -> fopBuilder.setStrictUserConfigValidation(value)
                    _StrictValidation -> fopBuilder.setStrictUserConfigValidation(value)
                    _UseCache -> {
                        if (!value) {
                            fopBuilder.fontManager.disableFontCache()
                        }
                    }
                    else -> Unit
                }
            }
        }

        for (key in floatOptions) {
            val value = options[key]?.underlyingValue?.stringValue?.toFloat() ?: defaultFloatOptions[key]
            if (value != null) {
                when (key) {
                    _SourceResolution -> fopBuilder.setSourceResolution(value)
                    _TargetResolution -> fopBuilder.setTargetResolution(value)
                    else -> Unit
                }
            }
        }

        fopFactory = fopBuilder.build()
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (!supportedContentTypes.contains(contentType)) {
            throw stepConfig.exception(XProcError.xcUnsupportedContentType(contentType))
        }

        val fodoc = S9Api.xdmToInputSource(stepConfig, document)
        val source = SAXSource(fodoc)

        val userAgent = fopFactory.newFOUserAgent()

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _Author -> userAgent.author = value
                    _Creator -> userAgent.creator = value
                    _Keywords -> userAgent.keywords = value
                    _Producer -> userAgent.producer = value
                    _Subject -> userAgent.subject = value
                    _Title -> userAgent.title = value
                    _CreationDate -> {
                        val df = DateFormat.getDateInstance()
                        val d = df.parse(value)
                        userAgent.setCreationDate(d)
                    }
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _Accessibility -> userAgent.setAccessibility(value)
                    _ConserveMemoryPolicy -> userAgent.setConserveMemoryPolicy(value)
                    _LocatorEnabled -> userAgent.isLocatorEnabled = value
                    else -> Unit
                }
            }
        }

        for (key in floatOptions) {
            val value = options[key]?.underlyingValue?.stringValue?.toFloat() ?: defaultFloatOptions[key]
            if (value != null) {
                when (key) {
                    _TargetResolution -> userAgent.targetResolution = value
                    else -> Unit
                }
            }
        }

        val fop = userAgent.newFop(contentType.toString(), out)
        val defHandler = fop.defaultHandler

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.transform(source, SAXResult(defHandler))
    }
}