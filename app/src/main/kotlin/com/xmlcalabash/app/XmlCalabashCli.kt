package com.xmlcalabash.app

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.util.DefaultMessagePrinter
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Verbosity
import com.xmlcalabash.util.VisualizerOutput
import net.sf.saxon.Configuration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import org.slf4j.MDC
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

    private lateinit var builder: XmlCalabashBuilder
    private lateinit var xmlCalabash: XmlCalabash
    private lateinit var commandLine: CommandLine
    private lateinit var cliPrinter: MessagePrinter
    private lateinit var cliExplain: ErrorExplanation
    private lateinit var stepConfig: InstructionConfiguration
    private var sawStdout = false

    private fun run(args: Array<out String>) {
        builder = XmlCalabashBuilder()
        cliPrinter = DefaultMessagePrinter(XmlCalabashBuilder.DEFAULT_CONSOLE_ENCODING)
        cliExplain = DefaultErrorExplanation(cliPrinter)
        commandLine = CommandLine.parse(args)
        if (commandLine.errors.isNotEmpty()) {
            abort(cliExplain, commandLine.errors)
        }

        if (commandLine.debug == true) {
            when (commandLine.verbosity) {
                null -> Unit
                Verbosity.TRACE -> MDC.put("LOG_LEVEL", "TRACE")
                Verbosity.DEBUG -> MDC.put("LOG_LEVEL", "DEBUG")
                Verbosity.INFO -> MDC.put("LOG_LEVEL", "INFO")
                Verbosity.WARN -> MDC.put("LOG_LEVEL", "WARN")
                Verbosity.ERROR -> MDC.put("LOG_LEVEL", "ERROR")
            }
        }

        loadConfiguration(commandLine.config)

        if (commandLine.verbosity != null) {
            builder.setVerbosity(commandLine.verbosity!!)
            if (commandLine.debug == true) {
                when (builder.getVerbosity()) {
                    Verbosity.TRACE -> MDC.put("LOG_LEVEL", "TRACE")
                    Verbosity.DEBUG -> MDC.put("LOG_LEVEL", "DEBUG")
                    Verbosity.INFO -> MDC.put("LOG_LEVEL", "INFO")
                    Verbosity.WARN -> MDC.put("LOG_LEVEL", "WARN")
                    Verbosity.ERROR -> MDC.put("LOG_LEVEL", "ERROR")
                }
            }
        }

        commandLine.pipe?.let { builder.setPipe(it) }
        commandLine.trace?.let { builder.setTrace(it) }
        commandLine.traceDocuments?.let { builder.setTraceDocuments(it) }
        commandLine.assertions?.let { builder.setAssertions(it) }
        commandLine.tryNamespaces?.let { builder.setTryNamespaces(it) }
        commandLine.useLocationHints?.let { builder.setUseLocationHints(it) }

        builder.setLicensed(builder.getLicensed() && commandLine.licensed)

        commandLine.debug?.let { builder.setDebug(it) }
        commandLine.debugger?.let { builder.setDebugger(it) }
        when (commandLine.visualizer) {
            null -> {
                // If the user didn't specify one on the command line, use the one from
                // the configuration file, unless --debugger has been specified, in which
                // case turn it off. Debugging and visualization don't play nicely together.
                if (builder.getDebugger()) {
                    builder.setVisualizer("silent", emptyMap())
                }
            }
            "silent", "plain", "detail" -> builder.setVisualizer(commandLine.visualizer!!, commandLine.visualizerOptions)
            else -> {
                cliPrinter.print("Unexpected visualizer: ${commandLine.visualizer}")
                builder.setVisualizer("silent", emptyMap())
            }
        }

        if (builder.getTrace() == null && builder.getTraceDocuments() != null) {
            builder.setTrace(builder.getTraceDocuments()!!.resolve("trace.xml"))
        }

        for (uri in commandLine.xmlSchemas) {
            builder.addXmlSchemaDocument(uri)
        }

        for (uri in commandLine.xmlCatalogs) {
            builder.addXmlCatalog(uri)
        }

        for (name in commandLine.initializers) {
            builder.addInitializer(name)
        }

        // It feels like this configuration should go somewhere else...but since
        // CoffeeSacks is now bundled, try to initialize it for the user...
        val csi = "org.nineml.coffeesacks.RegisterCoffeeSacks"
        if (csi !in commandLine.initializers) {
            builder.addInitializer(csi, ignoreErrors = true)
        }

        val moon = Moon.illumination()
        if (moon > builder.mpt) {
            if (moon > 0.99) {
                logger.warn { "The moon is full." }
            } else {
                logger.warn { "The moon is ${"%3.1f".format(moon * 100.0)}% full." }
            }
        }

        xmlCalabash = builder.build()

        val xprocParser = xmlCalabash.newXProcParser()
        stepConfig = xprocParser.builder.stepConfig
        cliExplain = stepConfig.errorExplanation

        if (commandLine.help || (commandLine.command == "run" && commandLine.pipeline == null && commandLine.step == null)) {
            help()
            return
        }

        if (commandLine.command == "version") {
            version()
            return
        }

        try {
            // N.B. It's illegal to shadow a static option name, so we can shove all the
            // options into the static options before we parse the pipeline. This is...odd
            // and, I expect, unsatisfactory in the long term. But I'm not going to try
            // to fix that today.
            evaluateOptions(xprocParser.builder, commandLine)

            val declstep = if (commandLine.pipeline != null) {
                xprocParser.parse(commandLine.pipeline!!.toURI(), commandLine.step)
            } else {
                val type = stepConfig.typeUtils.parseQName(commandLine.step!!, commandLine.namespaces)
                constructWrapper(type)
            }

            val runtime = declstep.runtime()
            val pipeline = runtime.executable()

            var explicitStdin: String? = null
            for ((port, uris) in commandLine.inputs) {
                for (pair in uris) {
                    if (pair.first == CommandLine.STDIO_URI) {
                        explicitStdin = port
                    }
                }
            }

            var implicitStdin: String? = null
            if (explicitStdin == null && xmlCalabash.config.pipe) {
                for ((port, input) in pipeline.inputManifold) {
                    if (input.primary && port !in commandLine.inputs) {
                        implicitStdin = port
                    }
                }
            }

            var explicitStdout: String? = null
            for ((port, output) in commandLine.outputs) {
                if (output.pattern == CommandLine.STDIO_NAME) {
                    explicitStdout = port
                }
            }

            var implicitStdout: String? = null
            if (xmlCalabash.config.pipe) {
                for ((port, output) in pipeline.outputManifold) {
                    if (output.primary) {
                        implicitStdout = port
                    }
                }
            }

            if (commandLine.pipelineGraphs != null) {
                val description = runtime.description()
                val vis = VisualizerOutput(xmlCalabash, description, commandLine.pipelineGraphs!!)
                if (xmlCalabash.config.graphviz == null) {
                    logger.warn { "Cannot create SVG, graphviz is not configured"}
                    vis.xml()
                } else {
                    vis.svg()
                }
            }

            if (commandLine.nogo) {
                stepConfig.messageReporter.debug { "Execution suppressed with --nogo" }
                exitProcess(0)
            }

            if (implicitStdin != null) {
                var ctype = implicitContentType(pipeline.inputManifold[implicitStdin]?.contentTypes)
                commandLine._inputs[implicitStdin] = mutableListOf(Pair(CommandLine.STDIO_URI, ctype))
            }

            val stdin = if (explicitStdin != null || implicitStdin != null) {
                val port = explicitStdin ?: implicitStdin!!
                var ctype = implicitContentType(pipeline.inputManifold[port]?.contentTypes)

                val inputList = commandLine.inputs[port]
                for (pair in inputList!!) {
                    if (pair.first == CommandLine.STDIO_URI) {
                        if (pair.second != MediaType.ANY) {
                            ctype = pair.second
                        }
                        break
                    }
                }
                val loader = DocumentLoader(pipeline.config, CommandLine.STDIO_URI)
                loader.load(System.`in`, ctype)
            } else {
                null
            }

            for ((port, uris) in commandLine.inputs) {
                for (pair in uris) {
                    if (pair.first == CommandLine.STDIO_URI) {
                        pipeline.input(port, stdin!!)
                    } else {
                        val props = DocumentProperties()
                        if (pair.second != MediaType.ANY) {
                            props[Ns.contentType] = pair.second.toString()
                        }
                        val doc = stepConfig.environment.documentManager.load(pair.first, pipeline.config, props)
                        pipeline.input(port, doc)
                    }
                }
            }

            if (implicitStdout != null && implicitStdout !in commandLine.outputs) {
                commandLine._outputs[implicitStdout] = OutputFilename(CommandLine.STDIO_NAME)
            }

            for ((port, output) in commandLine.outputs) {
                if (!pipeline.outputManifold.containsKey(port)) {
                    throw XProcError.xiNoSuchOutputPort(port).exception()
                }
                if (output.pattern == "-") {
                    if (sawStdout) {
                        throw XProcError.xiAtMostOneStdout().exception()
                    }
                    sawStdout = true
                }
            }

            val optManager = xprocParser.builder.staticOptionsManager
            for ((name, value) in optManager.useWhenOptions) {
                if (name !in optManager.staticOptions) {
                    pipeline.option(name, XProcDocument.ofValue(value, stepConfig, MediaType.OCTET_STREAM, DocumentProperties()))
                }
            }

            pipeline.receiver = FileOutputReceiver(xmlCalabash, stepConfig.processor, pipeline.outputManifold, commandLine.outputs, explicitStdout ?: implicitStdout)
            pipeline.run()
        } catch (ex: Exception) {
            if (ex is XProcException) {
                if (ex.error.code == NsErr.xi(XProcError.DEBUGGER_ABORT)) {
                    exitProcess(1)
                }
                abort(cliExplain, ex)
            } else {
                if (commandLine.verbosity != null && commandLine.verbosity!! <= Verbosity.DEBUG) {
                    ex.printStackTrace()
                }
                System.err.println(ex)
                exitProcess(1)
            }
        }
    }

    private fun implicitContentType(types: List<MediaType>?): MediaType {
        if (types == null) {
            return MediaType.XML
        }
        for (type in types) {
            if (type.inclusive) {
                if (type.mediaType == "*" || type.mediaSubtype == "*") {
                    when (type.suffix) {
                        "xml" -> return MediaType.XML
                        "json" -> return MediaType.JSON
                    }
                    if (type.mediaType == "text") {
                        return MediaType.TEXT
                    }
                } else {
                    return type
                }
            }
        }

        return MediaType.XML
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

        val processor = stepConfig.processor

        val compiler = processor.newXPathCompiler()
        compiler.baseURI = UriUtils.cwdAsUri()
        compiler.isSchemaAware = processor.isSchemaAware
        for ((name, namespace) in nsmap) {
            compiler.declareNamespace(name, namespace.toString())
        }

        val implicitParameterName = pipelineBuilder.stepConfig.xmlCalabashConfig.implicitParameterName
        val mapOptions = mutableMapOf<QName, XdmValue>()

        for ((name, initializers) in commandLine.options) {
            var mapName: QName? = null
            val ccpos = name.indexOf("::")
            val qname = if (ccpos >= 0) {
                if (ccpos > 0) {
                    mapName = stepConfig.typeUtils.parseQName(name.substring(0, ccpos), nsmap)
                } else {
                    mapName = implicitParameterName
                }
                stepConfig.typeUtils.parseQName(name.substring(ccpos + 2), nsmap)
            } else {
                stepConfig.typeUtils.parseQName(name, nsmap)
            }

            var value: XdmValue? = if (mapName != null) {
                mapOptions[qname]
            } else {
                null
            }

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

            if (mapName != null) {
                mapOptions[qname] = value!!
            } else {
                pipelineBuilder.option(qname, value!!)
            }
        }

        if (mapOptions.isNotEmpty()) {
            pipelineBuilder.option(implicitParameterName!!, stepConfig.typeUtils.asXdmMap(mapOptions))
        }
    }

    private fun loadConfiguration(commandLineConfig: File?) {
        val configLocations = mutableListOf<File>()
        commandLineConfig?.let { configLocations.add(it) }
        configLocations.add(File(UriUtils.cwdAsUri().resolve(".xmlcalabash3").path))
        configLocations.add(File(UriUtils.homeAsUri().resolve(".xmlcalabash3").path))
        for (config in configLocations) {
            if (config.exists() && config.isFile) {
                val loader = ConfigurationLoader(builder)
                loader.load(config)
                return
            }
        }
    }

    private fun abort(errorExplanation: ErrorExplanation, error: XProcException) {
        abort(errorExplanation, listOf(error))
    }

    private fun abort(errorExplanation: ErrorExplanation, errors: List<XProcException>) {
        val verbosity = commandLine.verbosity ?: Verbosity.INFO

        for (error in errors) {
            errorExplanation.report(error.error)
            if (commandLine.explainErrors) {
                errorExplanation.reportExplanation(error.error)
            }
        }

        if (verbosity <= Verbosity.DEBUG) {
            errors[0].printStackTrace()
            if (errors[0].cause != null && errors[0].cause != errors[0]) {
                errors[0].cause!!.printStackTrace()
            }
        } else {
            if (errors[0].cause != null && errors[0].cause != errors[0]) {
                val message = errors[0].cause!!.message
                if (message != null) {
                    stepConfig.messagePrinter.println(message)
                }
            }
        }

        exitProcess(1)
    }

    private fun help() {
        val stream = XmlCalabashCli::class.java.getResourceAsStream("/com/xmlcalabash/help.txt")
        if (stream == null) {
           stepConfig.messagePrinter.println("Error: help is not available.")
            return
        }
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        for (line in reader.lines()) {
            stepConfig.messagePrinter.println(line)
        }
    }

    private fun version() {
        val proc = stepConfig.processor
        val license = proc.underlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)
                || proc.underlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)
        var edition = proc.saxonEdition
        if (!license) {
            if (javaClassExists("com.saxonica.config.EnterpriseConfiguration")) {
                edition = "EE"
            } else if (javaClassExists("com.saxonica.config.ProfessionalConfiguration")) {
                edition = "PE"
            }
        }

        val deplist = XmlCalabashBuildConfig.DEPENDENCIES.keys.toList().sorted()

        if (xmlCalabash.config.verbosity <= Verbosity.DEBUG) {
            println("PRODUCT_NAME=${XmlCalabashBuildConfig.PRODUCT_NAME}")
            println("VERSION=${XmlCalabashBuildConfig.VERSION}")
            println("BUILD_DATE=${XmlCalabashBuildConfig.BUILD_DATE}")
            println("BUILD_ID=${XmlCalabashBuildConfig.BUILD_ID}")
            println("SAXON_EDITION=${proc.saxonEdition}")
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

            stepConfig.messagePrinter.print("${XmlCalabashBuildConfig.PRODUCT_NAME} version ${XmlCalabashBuildConfig.VERSION} ")
            stepConfig.messagePrinter.println("(build ${XmlCalabashBuildConfig.BUILD_ID}, ${dateFormatter.format(date)})")
            stepConfig.messagePrinter.println("Running with Saxon ${proc.saxonEdition} version ${proc.saxonProductVersion}")
            if (edition != proc.saxonEdition) {
                if (xmlCalabash.config.licensed) {
                    println("(You appear to have ${edition}; perhaps a license wasn't found?)")
                } else {
                    println("(You appear to have ${edition} but the license is explicitly disabled.)")
                }
            }
        }
    }

    private fun javaClassExists(klass: String): Boolean {
        try {
            if (Class.forName(klass) != null) {
                return true
            }
        } catch (_: ClassNotFoundException) {
            return false
        }
        return false
    }

    private fun constructWrapper(xstep: QName): DeclareStepInstruction {
        val stepDecl = findDeclaration(xstep)
        val step = stepDecl.second

        val builder = xmlCalabash.newPipelineBuilder(3.1)
        val decl = builder.newDeclareStep()

        if (stepDecl.first != null) {
            decl.import(stepDecl.first!!)
        }

        for (input in step.inputs()) {
            decl.input(input.port, input.primary == true, input.sequence == true)
        }
        for (output in step.outputs()) {
            val decloutput = decl.output(output.port, output.primary == true, output.sequence == true)
            decloutput.pipe = output.port

        }
        for (option in step.getOptions()) {
            val decloption = decl.option(option.name)
            decloption.values = option.values
            decloption.asType = option.asType
            decloption.required = option.required
            decloption.select = option.select
            decloption.specialType = option.specialType
        }

        val innerstep = decl.atomicStep(step.type!!)
        for (input in step.inputs()) {
            val wi = innerstep.withInput(input.port)
            wi.pipe = input.port
        }
        for (option in step.getOptions()) {
            val wo = innerstep.withOption(option.name)
            wo.empty()
            wo.select = XProcExpression.select(innerstep.stepConfig, "\$${option.name}")
        }

        return decl
    }

    private fun findDeclaration(type: QName): Pair<LibraryInstruction?, DeclareStepInstruction> {
        val pstep = stepConfig.standardSteps[type]
        if (pstep != null) {
            return Pair(null, pstep)
        }

        for (provider in DocumentResolverServiceProvider.providers()) {
            val resolver = provider.create()
            for (uri in resolver.resolvableLibraryUris()) {
                val xprocParser = xmlCalabash.newXProcParser()
                val library = xprocParser.parseLibrary(uri)
                for (decl in library.children.filterIsInstance<DeclareStepInstruction>()) {
                    if (decl.type == type) {
                        return Pair(library, decl)
                    }
                }
            }
        }

        throw XProcError.xsMissingStepDeclaration(type).exception()
    }
}