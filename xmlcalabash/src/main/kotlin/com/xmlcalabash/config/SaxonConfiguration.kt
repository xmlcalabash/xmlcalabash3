package com.xmlcalabash.config

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.functions.*
import com.xmlcalabash.runtime.RuntimeExecutionContext
import com.xmlcalabash.runtime.ValueConverter
import com.xmlcalabash.util.SaxonValueConverter
import net.sf.saxon.Configuration
import net.sf.saxon.functions.FunctionLibrary
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.lib.FeatureIndex
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.Xslt30Transformer
import org.xml.sax.InputSource
import java.io.File
import java.net.URI
import javax.xml.transform.sax.SAXSource

class SaxonConfiguration private constructor(
    val configuration: Configuration,
    val rteContext: RuntimeExecutionContext,
    val valueConverter: ValueConverter,
    val processor: Processor
): ExecutionContext by rteContext, ValueConverter by valueConverter {
    companion object {
        private var _id = 0
        fun newInstance(xmlCalabash: XmlCalabash, rteContext: RuntimeExecutionContext): SaxonConfiguration {
            // There's a little bit of chicken-and-egg here as I want to make a SaxonValueConverter,
            // but I need a Processor to do that.
            val newConfiguration = newConfiguration(xmlCalabash)
            val processor = Processor(newConfiguration)
            val converter = SaxonValueConverter(processor)

            val saxon = SaxonConfiguration(newConfiguration, rteContext, converter, processor)

            saxon._xmlCalabash = xmlCalabash
            saxon.configurationFile = xmlCalabash.xmlCalabashConfig.saxonConfigurationFile
            saxon.configurationProperties.putAll(xmlCalabash.xmlCalabashConfig.saxonConfigurationProperties)
            saxon.configureProcessor(processor)
            return saxon
        }

        private fun newConfiguration(xmlCalabash: XmlCalabash): Configuration {
            val newConfiguration = if (xmlCalabash.xmlCalabashConfig.saxonConfigurationFile == null) {
                Configuration.newLicensedConfiguration()
            } else {
                val source = SAXSource(InputSource(xmlCalabash.xmlCalabashConfig.saxonConfigurationFile!!.toURI().toString()))
                Configuration.readConfiguration(source)
            }

            for ((key, value) in xmlCalabash.xmlCalabashConfig.saxonConfigurationProperties) {
                val data = FeatureIndex.getData(key) ?: throw XProcError.xiUnrecognizedSaxonConfigurationProperty(key).exception()
                if (data.type == java.lang.Boolean::class.java) {
                    if (value == "true" || value == "false") {
                        newConfiguration.setConfigurationProperty(key, value == "true")
                    } else {
                        throw XProcError.xiInvalidSaxonConfigurationProperty(key, value).exception()
                    }
                } else {
                    newConfiguration.setConfigurationProperty(key, value)
                }
            }

            xmlCalabash.xmlCalabashConfig.saxonConfigurer(newConfiguration)
            return newConfiguration
        }
    }

    val id = ++_id
    private lateinit var _xmlCalabash: XmlCalabash
    private var configurationFile: File? = null
    private val configurationProperties = mutableMapOf<String,String>()
    private var _xpathTransformer: Xslt30Transformer? = null

    val xmlCalabash: XmlCalabash
        get() = _xmlCalabash

    // This is on the SaxonConfiguration because it needs to use a compatible configuration
    // and I want to cache it somewhere so that it doesn't have to be parsed for *every* expression
    internal val xpathTransformer: Xslt30Transformer
        get() {
            if (_xpathTransformer == null) {
                var styleStream = SaxonConfiguration::class.java.getResourceAsStream("/com/xmlcalabash/xpath.xsl")
                var styleSource = SAXSource(InputSource(styleStream))
                var xsltCompiler = processor.newXsltCompiler()
                var xsltExec = xsltCompiler.compile(styleSource)
                _xpathTransformer = xsltExec.load30()
            }
            return _xpathTransformer!!
        }

    private val functionLibraries = mutableMapOf<URI, FunctionLibrary>()
    private val standardExtensionFunctions = listOf<(SaxonConfiguration) -> ExtensionFunctionDefinition>(
        { config -> DocumentPropertyFunction(config) },
        { config -> DocumentPropertiesFunction(config) },
        { config -> ErrorFunction(config) },
        { config -> FunctionLibraryImportableFunction(config) },
        { config -> IterationPositionFunction(config) },
        { config -> IterationSizeFunction(config) },
        { config -> StepAvailableFunction(config) },
        { config -> SystemPropertyFunction(config) },
        { config -> UrifyFunction(config) }
    )

    fun newConfiguration(): SaxonConfiguration {
        val newConfig = newConfiguration(xmlCalabash)
        val processor = Processor(newConfig)
        val saxon = SaxonConfiguration(newConfig, rteContext, SaxonValueConverter(processor), processor)
        saxon.configuration.namePool = configuration.namePool
        saxon.configuration.documentNumberAllocator = configuration.documentNumberAllocator

        saxon._xmlCalabash = xmlCalabash
        saxon.configurationFile = configurationFile
        saxon.configurationProperties.putAll(configurationProperties)
        saxon.functionLibraries.putAll(functionLibraries)
        saxon.configureProcessor(processor)
        return saxon
    }

    fun newProcessor(): Processor {
        val newproc = Processor(configuration)
        configureProcessor(newproc)
        return newproc
    }

    internal fun addFunctionLibrary(href: URI, flib: FunctionLibrary) {
        if (functionLibraries.containsKey(href)) {
            return
        }
        functionLibraries[href] = flib
        loadFunctionLibrary(href, flib)
    }

    private fun loadFunctionLibrary(href: URI, flib: FunctionLibrary) {
        // Is this the best way?
        val pc = Class.forName("com.saxonica.config.ProfessionalConfiguration")
        val fc = Class.forName("net.sf.saxon.functions.FunctionLibrary")
        val setBinder = pc.getMethod("setExtensionBinder", String::class.java, fc)
        setBinder.invoke(configuration, "xmlcalabash:${href}", flib)
    }

    // ============================================================

    private fun configureProcessor(processor: Processor) {
        for (function in standardExtensionFunctions) {
            processor.registerExtensionFunction(function(this))
        }

        for ((href, flib) in functionLibraries) {
            loadFunctionLibrary(href, flib)
        }
    }
}