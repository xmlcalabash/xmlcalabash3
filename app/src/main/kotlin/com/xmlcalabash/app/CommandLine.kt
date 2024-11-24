package com.xmlcalabash.app

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import java.io.File
import java.net.URI

/**
 * Command line parser.
 *
 * This class parses an array of strings (as might appear as arguments on the command line). The resulting
 * object has properties for all the options provided. If an option is not provided, and it is not required,
 * the value of the corresponding property will be `null`.
 *
 * @param args The array of command line arguments to parse.
 */

class CommandLine private constructor(val args: Array<out String>) {
    /**
     * Provides a static method to create a [CommandLine] from a list of arguments.
     */
    companion object {
        /**
         * Parse a set of arguments.
         *
         * The caller must check the [errors] property. If it is empty, the parse was successful.
         */
        fun parse(args: Array<out String>): CommandLine {
            val cli = CommandLine(args)
            cli.parse()
            return cli
        }
    }

    private val validCommands = listOf("version", "run", "help")
    private var _command: String = "run"
    private var _config: File? = null
    private var _pipelineGraph: String? = null
    private var _schemaAware = false
    private var _pipelineDescription: String? = null
    private var _debug: Boolean? = null
    private var _verbosity: Verbosity? = null
    private var _help = false
    private var _errors = mutableListOf<XProcException>()
    private var _inputs = mutableMapOf<String, MutableList<URI>>()
    private var _outputs = mutableMapOf<String, OutputFilename>()
    private var _options = mutableMapOf<String,List<String>>()
    private var _namespaces = mutableMapOf<String, NamespaceUri>()
    private var _pipeline: File? = null
    private var _step: String? = null

    /** The command. */
    val command: String
        get() = _command

    /** The configuration file specified. */
    val config: File?
        get() = _config

    /** The pipeline graph description output filename. */
    val pipelineGraph: String?
        get() = _pipelineGraph

    /** Enable schema-aware processing?
     * <p>This feature requires Saxon EE.</p>
     */
    val schemaAware: Boolean
        get() = _schemaAware

    /** The pipeline description output filename. */
    val pipelineDescription: String?
        get() = _pipelineDescription

    /** Enable debugging output? */
    val debug: Boolean?
        get() = _debug

    /** How chatty shall we be? */
    val verbosity: Verbosity?
        get() = _verbosity

    /** Display command line help instructions?
     *
     * If this is true, a summary of the command line options will be presented,
     * but no pipeline will be run.
     */
    val help: Boolean
        get() = _help || _command == "help"

    /** Did any parse errors occur? */
    val errors: List<XProcException>
        get() = _errors

    /** The pipeline inputs.
     * <p>The inputs are a map from input port name to a (list of) URI(s).</p>
     */
    val inputs: Map<String,List<URI>>
        get() = _inputs

    /** The pipeline outputs.
     * <p>The outputs are a map from output port name to output filenames.
     * The [OutputFilename] class is responsible for determining how multiple outputs
     * on a single port are serialized.</p>
     */
    val outputs: Map<String,OutputFilename>
        get() = _outputs

    /** The pipeline options.
     * <p>This is a map from option names to values. The option names must be QNames
     * expressed as <code><a href="https://www.w3.org/TR/xpath-31/#doc-xpath31-EQName">EQName</a></code>s
     * or using namespace bindings provided by the <code>--namespace</code> options.</p>
     * <p>If the value begins with a <code>?</code>, the value after the leading question mark
     * will be evaluated as an XPath expression and the resulting value becomes the value of the option.
     * Otherwise, the value of the option is the string value as an <code>xs:untypedAtomic</code>.</p>
     * <p>If an option is given multiple times, it will have a value that is the sequence of values
     * provided.</p>
     */
    val options: Map<String, List<String>>
        get() = _options

    /** In-scope namespace declarations for argument parsing. */
    val namespaces: Map<String, NamespaceUri>
        get() = _namespaces

    /** The pipeline or library document containing the pipeline to run. */
    val pipeline: File?
        get() = _pipeline

    /** The step (in a library) to run.
     * <p>If the [pipeline] is an <code>p:library</code>, the [step] option identifies
     * a step in the library to run. The step is identified by name, not by type.</p>
     */
    val step: String?
        get() = _step

    private val arguments = listOf(
        ArgumentDescription("--input", listOf("-i"), ArgumentType.STRING) { it -> parseInput(it) },
        ArgumentDescription("--output", listOf("-o"), ArgumentType.STRING) { it -> parseOutput(it) },
        ArgumentDescription("--namespace", listOf("-ns"), ArgumentType.STRING) { it -> parseNamespace(it) },
        ArgumentDescription("--configuration", listOf("-c", "--config"), ArgumentType.EXISTING_FILE) { it -> _config = File(it) },
        ArgumentDescription("--step", listOf(), ArgumentType.STRING) { it -> _step = it },
        ArgumentDescription("--graph", listOf(), ArgumentType.FILE) { it -> _pipelineGraph = it },
        ArgumentDescription("--description", listOf(), ArgumentType.FILE) { it -> _pipelineDescription = it },
        ArgumentDescription("--schema-aware", listOf("-a"), ArgumentType.BOOLEAN, "true") { it -> _schemaAware = it == "true" },
        ArgumentDescription("--debug", listOf("-D"), ArgumentType.BOOLEAN, "true") { it -> _debug = it == "true" },
        ArgumentDescription("--help", listOf(), ArgumentType.BOOLEAN, "true") { it -> _help = it == "true" },
        ArgumentDescription("--verbosity", listOf("-V"),
            ArgumentType.STRING, "detail", listOf("quiet", "detail", "progress", "normal", "warning")) { it ->
            _verbosity = when(it) {
                "quiet" -> Verbosity.QUIET
                "detail" -> Verbosity.DETAIL
                "progress" -> Verbosity.PROGRESS
                "normal" -> Verbosity.NORMAL
                "warning" -> Verbosity.WARNING
                else -> Verbosity.NORMAL
            }}
    )

    private fun parse() {
        if (args.isEmpty()) {
            _command = "help"
            return
        }

        for ((index, opt) in args.withIndex()) {
            if (index == 0 && opt in validCommands) {
                _command = opt
                continue
            }

            val pos = opt.indexOf(':')
            val option = if (pos >= 0) {
                opt.substring(0, pos)
            } else {
                opt
            }
            val suppliedValue = if (pos >= 0) {
                opt.substring(pos + 1)
            } else {
                null
            }

            var processed = false
            for (arg in arguments) {
                if (option == arg.name || arg.synonyms.contains(option)) {
                    var value = suppliedValue ?: arg.default
                    if (value == null) {
                        _errors.add(XProcError.xiCliValueRequired(option).exception())
                        continue
                    }

                    if (arg.valid.isNotEmpty() && !arg.valid.contains(value)) {
                        _errors.add(XProcError.xiCliInvalidValue(option, value).exception())
                        continue
                    }

                    when (arg.type) {
                        ArgumentType.STRING -> Unit
                        ArgumentType.BOOLEAN -> {
                            if (value != "true" && value != "false") {
                                _errors.add(XProcError.xiCliInvalidValue(option, value).exception())
                                continue
                            }
                        }
                        ArgumentType.FILE -> {
                            val file = File(value)
                            if (file.exists() && !file.isFile) {
                                _errors.add(XProcError.xiCliInvalidValue(option, value).exception())
                                continue
                            }
                        }
                        ArgumentType.EXISTING_FILE -> {
                            val file = File(value)
                            if (!file.exists() || !file.isFile || !file.canRead()) {
                                _errors.add(XProcError.xiCliInvalidValue(option, value).exception())
                                continue
                            }
                        }
                        ArgumentType.URI -> {
                            value = UriUtils.cwdAsUri().resolve(value).toString()
                        }
                    }
                    processed = true
                    arg.process(value)
                    break
                }
            }

            if (!processed) {
                if (option.startsWith("-")) {
                    _errors.add(XProcError.xiCliUnrecognizedOption(option).exception())
                } else if (opt.contains("=")) {
                    parseOptionParam(opt)
                } else {
                    if (_pipeline != null) {
                        _errors.add(XProcError.xiCliMoreThanOnePipeline(_pipeline.toString(), opt).exception())
                    } else {
                        try {
                            _pipeline = parseFile(opt)
                        } catch (ex: XProcException) {
                            _errors.add(ex)
                        }
                    }
                }
            }
        }
    }

    private fun split(arg: String, type: String): Pair<String, String> {
        val pos = arg.indexOf("=")
        if (pos <= 0) {
            throw XProcError.xiCliMalformedOption(type, arg).exception()
        }
        return Pair(arg.substring(0, pos).trim(), arg.substring(pos + 1).trim())
    }

    private fun parseInput(arg: String) {
        // -i:port=path
        val (port, href) = split(arg, "input")
        if (!_inputs.containsKey(port)) {
            _inputs.put(port, mutableListOf())
        }
        _inputs[port]!!.add(UriUtils.cwdAsUri().resolve(href))
    }

    private fun parseOutput(arg: String) {
        // -i:port=path
        val (port, filename) = split(arg, "output")
        if (_outputs.containsKey(port)) {
            throw XProcError.xiCliDuplicateOutputFile(filename).exception()
        }
        _outputs[port] = OutputFilename(filename)
    }

    private fun parseNamespace(arg: String) {
        // -ns:path or -ns:prefix=path
        val eqpos = arg.indexOf("=")
        val prefix = if (eqpos >= 0) {
            arg.substring(0, eqpos)
        } else {
            ""
        }
        val uri = if (eqpos >= 0) {
            arg.substring(eqpos + 1)
        } else {
            arg
        }

        if (_namespaces.containsKey(prefix)) {
            throw XProcError.xiCliDuplicateNamespace(prefix).exception()
        }

        _namespaces[prefix] = NamespaceUri.of(uri)
    }

    private fun parseOptionParam(arg: String) {
        val (name, value) = split(arg, "option")
        val values = mutableListOf<String>()
        values.addAll(_options[name] ?: emptyList())
        values.add(value)
        _options[name] = values
    }

    private fun parseFile(arg: String): File {
        val pfile = File(arg)
        if (!pfile.exists() || !pfile.isFile() || !pfile.canRead()) {
            throw XProcError.xiUnreadableFile(arg).exception()
        }
        return pfile
    }

    internal inner class ArgumentDescription(val name: String,
                                    val synonyms: List<String>,
                                    val type: ArgumentType,
                                    val default: String? = null,
                                    val valid: List<String> = listOf(),
                                    val process: (String) -> Unit)

    internal enum class ArgumentType {
        STRING, URI, FILE, EXISTING_FILE, BOOLEAN
    }
}