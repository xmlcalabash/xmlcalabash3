package com.xmlcalabash.config

import com.xmlcalabash.datamodel.PipelineEnvironment
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.functions.*
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
    val environment: PipelineEnvironment,
    val configuration: Configuration,
    val processor: Processor
) {
    companion object {
        private var _id = 0
        fun newInstance(environment: PipelineEnvironment): SaxonConfiguration {
            // There's a little bit of chicken-and-egg here as I want to make a SaxonValueConverter,
            // but I need a Processor to do that.
            val newConfiguration = newConfiguration(environment.xmlCalabash)
            val processor = Processor(newConfiguration)

            val saxon = SaxonConfiguration(environment, newConfiguration, processor)

            saxon._xmlCalabash = environment.xmlCalabash
            saxon.configurationFile = environment.xmlCalabash.xmlCalabashConfig.saxonConfigurationFile
            saxon.configurationProperties.putAll(environment.xmlCalabash.xmlCalabashConfig.saxonConfigurationProperties)
            saxon.configureProcessor(processor)
            return saxon
        }

        private fun newConfiguration(xmlCalabash: XmlCalabash): Configuration {
            val newConfiguration = if (xmlCalabash.xmlCalabashConfig.saxonConfigurationFile == null) {
                if (xmlCalabash.xmlCalabashConfig.licensed) {
                    Configuration.newLicensedConfiguration()
                } else {
                    Configuration()
                }
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
                xsltCompiler.isSchemaAware = processor.isSchemaAware
                var xsltExec = xsltCompiler.compile(styleSource)
                _xpathTransformer = xsltExec.load30()
            }
            return _xpathTransformer!!
        }

    private val functionLibraries = mutableListOf<Pair<URI,FunctionLibrary>>()
    private val standardExtensionFunctions = listOf<(SaxonConfiguration) -> ExtensionFunctionDefinition>(
        { config -> DocumentPropertyFunction(config) },
        { config -> DocumentPropertiesFunction(config) },
        { config -> ErrorFunction(config) },
        { config -> FunctionLibraryImportableFunction(config) },
        { config -> IterationPositionFunction(config) },
        { config -> IterationSizeFunction(config) },
        { config -> StepAvailableFunction(config) },
        { config -> SystemPropertyFunction(config) },
        { config -> UrifyFunction(config) },
        { config -> LookupUriFunction(config) }
    )

    fun newConfiguration(): SaxonConfiguration {
        return newConfiguration(environment)
    }

    fun newConfiguration(environment: PipelineEnvironment): SaxonConfiguration {
        val newConfig = newConfiguration(xmlCalabash)
        val processor = Processor(newConfig)
        val saxon = SaxonConfiguration(environment, newConfig, processor)
        saxon.configuration.namePool = configuration.namePool
        saxon.configuration.documentNumberAllocator = configuration.documentNumberAllocator

        saxon._xmlCalabash = xmlCalabash
        saxon.configurationFile = configurationFile
        saxon.configurationProperties.putAll(configurationProperties)
        saxon.functionLibraries.addAll(functionLibraries)
        saxon.configureProcessor(processor)
        return saxon
    }

    fun newProcessor(): Processor {
        val newproc = Processor(configuration)
        configureProcessor(newproc)
        return newproc
    }

    internal fun addFunctionLibrary(href: URI, flib: FunctionLibrary) {
        for (current in functionLibraries) {
            if (current.first == href) {
                return
            }
        }

        functionLibraries.add(Pair(href,flib))
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

        // So that libraries loaded later "come first" when searching...
        for (flib in functionLibraries.reversed()) {
            loadFunctionLibrary(flib.first, flib.second)
        }
    }
}