package com.xmlcalabash.app

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.config.XmlCalabashConfiguration
import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.util.*
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * Run the XML Calabash application with the specified arguments.
 * @throws XProcException if an error occurs.
 */
class XmlCalabashCli private constructor() {
    companion object {
        fun run(args: Array<out String>) {
            val cli = XmlCalabashCli()
            cli.run(args)
        }
    }

    lateinit private var xmlCalabash: XmlCalabash
    lateinit private var commandLine: CommandLine
    lateinit private var config: XmlCalabashConfiguration
    lateinit private var stepConfig: InstructionConfiguration

    private fun run(args: Array<out String>) {
        var errorExplanation: ErrorExplanation = DefaultErrorExplanation()
        val inputManifold = mutableMapOf<String, RuntimePort>()
        val outputManifold = mutableMapOf<String, RuntimePort>()
        val optionManifold = mutableMapOf<QName, RuntimeOption>()

        commandLine = CommandLine.parse(args)
        if (commandLine.errors.isNotEmpty()) {
            abort(errorExplanation, commandLine.errors)
        }

        try {
            config = loadConfiguration(commandLine.config)

            config.debug = commandLine.debug ?: config.debug
            config.verbosity = commandLine.verbosity ?: config.verbosity

            xmlCalabash = XmlCalabash.newInstance(config)

            if (commandLine.help || (commandLine.command == "run" && commandLine.pipeline == null)) {
                help()
                return
            }

            if (commandLine.command == "version") {
                version()
                return
            }

            val moon = Moon.illumination()
            if (moon > config.mpt) {
                if (moon > 0.99) {
                    logger.warn { "The moon is full." }
                } else {
                    logger.warn { "The moon is ${"%3.1f".format(moon * 100.0)}% full." }
                }
            }

            val xprocParser = xmlCalabash.newXProcParser()
            stepConfig = xprocParser.builder.stepConfig
            errorExplanation = stepConfig.environment.errorExplanation

            evaluateOptions(xprocParser.builder, commandLine)

            val declstep = xprocParser.parse(commandLine.pipeline!!.toURI(), commandLine.step)
            val runtime = declstep.runtime()
            val pipeline = runtime.executable()

            if (commandLine.pipelineDescription != null || commandLine.pipelineGraph != null) {
                val description = runtime.description()
                if (commandLine.pipelineDescription != null) {
                    VisualizerOutput.xml(description, commandLine.pipelineDescription!!)
                }

                if (commandLine.pipelineGraph != null) {
                    if (config.graphviz == null) {
                        logger.warn { "Cannot create SVG descriptions, graphviz is not configured"}
                    } else {
                        VisualizerOutput.svg(description, commandLine.pipelineGraph!!, config.graphviz!!.absolutePath)
                    }
                }
            }

            inputManifold.putAll(pipeline.inputManifold)
            outputManifold.putAll(pipeline.outputManifold)
            for ((port, uris) in commandLine.inputs) {
                for (uri in uris) {
                    val doc = stepConfig.environment.documentManager.load(uri, pipeline.config)
                    pipeline.input(port, doc)
                }
            }

            optionManifold.putAll(pipeline.optionManifold)
            //val staticOpts = runtime.pipeline!!.staticOptions
            for ((name, value) in xprocParser.builder.staticOptionsManager.useWhenOptions) {
                //if (name !in staticOpts) {
                //    runtime.option(name, value)
                //}
                pipeline.option(name, XProcDocument.ofValue(value, stepConfig, MediaType.ANY, DocumentProperties()))
            }

            pipeline.receiver = FileOutputReceiver(xmlCalabash.saxonConfig.processor, commandLine.outputs)

            pipeline.run()
        } catch (ex: Exception) {
            if (commandLine.debug == true) {
                ex.printStackTrace()
            }
            if (ex is XProcException) {
                abort(errorExplanation, ex)
            } else {
                System.err.println(ex)
                exitProcess(1)
            }
        }
    }

    private fun evaluateOptions(pipelineBuilder: PipelineBuilder, commandLine: CommandLine) {
        val defaults = mutableMapOf<NamespaceUri, String>(
            NsXs.namespace to "xs",
            NsFn.namespace to "fn",
            NsFn.mapNamespace to "map",
            NsFn.arrayNamespace to "array",
            NsFn.mathNamespace to "math",
            NamespaceUri.of("http://saxon.sf.net/") to "saxon"
        )

        val nsmap = mutableMapOf<String, NamespaceUri>()
        for ((key, value) in commandLine.namespaces) {
            nsmap[key] = value
            defaults.remove(value)
        }
        for ((value, key) in defaults) {
            nsmap[key] = value
        }

        val processor = xmlCalabash.saxonConfig.newProcessor()

        val compiler = processor.newXPathCompiler()
        for ((name, namespace) in nsmap) {
            compiler.declareNamespace(name, namespace.toString())
        }

        for ((name, initializers) in commandLine.options) {
            val qname = stepConfig.parseQName(name, nsmap)
            var value: XdmValue? = null
            for (initializer in initializers) {
                val ivalue = if (initializer.startsWith("?")) {
                    val exec = compiler.compile(initializer.substring(1))
                    val selector = exec.load()
                    selector.evaluate()
                } else {
                    XdmAtomicValue(initializer, ItemType.UNTYPED_ATOMIC)
                }
                if (value == null) {
                    value = ivalue
                } else {
                    value = value.append(ivalue)
                }
            }
            pipelineBuilder.option(qname, value!!)
        }
    }

    private fun loadConfiguration(commandLineConfig: File?): XmlCalabashConfiguration {
        val configLocations = mutableListOf<File>()
        commandLineConfig?.let { configLocations.add(it) }
        configLocations.add(File(UriUtils.cwdAsUri().resolve(".xmlcalabash3").path))
        configLocations.add(File(UriUtils.homeAsUri().resolve(".xmlcalabash3").path))
        for (config in configLocations) {
            if (config.exists() && config.isFile) {
                return ConfigurationLoader.load(config)
            }
        }
        return DefaultXmlCalabashConfiguration()
    }

    private fun abort(errorExplanation: ErrorExplanation, error: XProcException) {
        abort(errorExplanation, listOf(error))
    }

    private fun abort(errorExplanation: ErrorExplanation, errors: List<XProcException>) {
        for (error in errors) {
            errorExplanation.message(error.error)
            if (commandLine.verbosity == Verbosity.DETAIL) {
                errorExplanation.explanation(error.error)
            }
        }

        if (commandLine.debug == true) {
            errors[0].printStackTrace()
        }

        exitProcess(1)
    }

    private fun help() {
        val stream = XmlCalabashCli::class.java.getResourceAsStream("/com/xmlcalabash/help.txt")
        if (stream == null) {
            print("Error: help is not available.")
            return
        }
        val reader = BufferedReader(InputStreamReader(stream))
        for (line in reader.lines()) {
            println(line)
        }
    }

    private fun version() {
        val proc = xmlCalabash.saxonConfig.processor
        val deplist = XmlCalabashBuildConfig.DEPENDENCIES.keys.toList().sorted()

        if (xmlCalabash.xmlCalabashConfig.debug) {
            println("PRODUCT_NAME=${XmlCalabashBuildConfig.PRODUCT_NAME}")
            println("VERSION=${XmlCalabashBuildConfig.VERSION}")
            println("BUILD_DATE=${XmlCalabashBuildConfig.BUILD_DATE}")
            println("BUILD_HASH=${XmlCalabashBuildConfig.BUILD_HASH}")
            println("SAXON=${proc.saxonEdition}")
            println("SAXON_LICENSE=${proc.isSchemaAware}")
            println("VENDOR_NAME=${XmlCalabashBuildConfig.VENDOR_NAME}")
            println("VENDOR_URI=${XmlCalabashBuildConfig.VENDOR_URI}")
            for (name in deplist) {
                val version = XmlCalabashBuildConfig.DEPENDENCIES[name]!!
                println("DEPENDENCY_${name}=${version}")
            }
        } else {
            val dateParser = SimpleDateFormat("yyyy-MM-dd")
            val dateInstant = dateParser.parse(XmlCalabashBuildConfig.BUILD_DATE).toInstant()
            val date = LocalDateTime.ofInstant(dateInstant, ZoneId.systemDefault())
            val dateFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

            print("${XmlCalabashBuildConfig.PRODUCT_NAME} version ${XmlCalabashBuildConfig.VERSION} ")
            println("(build ${XmlCalabashBuildConfig.BUILD_HASH}, ${dateFormatter.format(date)})")
            print("Running with Saxon ${proc.saxonEdition} version ${proc.saxonProductVersion}")
            if (proc.saxonEdition != "HE") {
                if (proc.isSchemaAware) {
                    print(" (with a license)")
                } else {
                    print(" (without a license)")
                }
            }
            println()
            
            if (xmlCalabash.xmlCalabashConfig.verbosity == Verbosity.DETAIL) {
                var sb = StringBuilder()
                sb.append("\nDependencies: ")
                for ((index, name) in deplist.withIndex()) {
                    sb.append(name).append("=").append(XmlCalabashBuildConfig.DEPENDENCIES[name]!!)
                    if (index < deplist.size - 1) {
                        sb.append(", ")
                    }
                    if (sb.toString().length > 64) {
                        println(sb.toString())
                        sb = StringBuilder()
                    }
                }
                if (sb.toString().isNotEmpty()) {
                    println(sb.toString())
                }
            }
        }
    }

}