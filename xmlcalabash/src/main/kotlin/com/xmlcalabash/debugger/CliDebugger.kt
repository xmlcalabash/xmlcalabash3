package com.xmlcalabash.debugger

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.graph.SubpipelineModel
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsXmlns
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.steps.*
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeefilter.InvisibleXmlParser
import org.nineml.coffeefilter.ParserOptions
import org.nineml.coffeefilter.trees.DataTreeBuilder
import java.nio.charset.StandardCharsets
import java.util.*

class CliDebugger(val runtime: XProcRuntime): Debugger {
    val terminal = TerminalBuilder.builder().dumb(true).build()
    val reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    val prompt = "> "

    var parser: InvisibleXmlParser? = null
    val stacks = mutableMapOf<Long, Stack<StackFrame>>()
    val breakpoints = mutableMapOf<String, MutableList<Breakpoint>>()
    var stepping = true
    var help = mutableListOf<String>()

    val localNamespaces = mutableMapOf<String, NamespaceUri>()
    val localVariables = mutableMapOf<QName, XdmValue>()

    lateinit var curFrame: StackFrame
    var stack: Stack<StackFrame> = Stack()
    var frameNumber = -1

    override fun startStep(step: AbstractStep) {
        if (parser == null) {
            init()
        }

        stack = stacks[Thread.currentThread().id] ?: Stack()
        stacks[Thread.currentThread().id] = stack
        curFrame = StackFrame(step)
        stack.push(curFrame)
        frameNumber = stack.size - 1

        var stopHere = stepping
        if (!stopHere) {
            for (bp in breakpoints[step.id] ?: emptyList()) {
                if (bp is UnconditionalBreakpoint) {
                    stopHere = true
                } else {
                    stopHere = evalBreakpoint(bp)
                }
                if (stopHere) {
                    break
                }
            }
        }

        if (stopHere) {
            cli(step)
        }
    }

    override fun endStep(step: AbstractStep) {
        stack.pop()
    }

    private fun cli(step: AbstractStep) {
        println("Debugger at ${step.id} / ${step.name}")
        if (curFrame.cx != "cx") {
            println("xmlns:${curFrame.cx} = ${NsCx.namespace}")
        }

        try {
            while (true) {
                val line = reader.readLine(prompt).trim()
                if (line == "") {
                    continue
                }

                val doc = parser!!.parse(line)
                if (!doc.succeeded()) {
                    println("Syntax error: ${line}")
                    continue
                }

                val opts = ParserOptions()
                opts.assertValidXmlNames = false
                opts.assertValidXmlCharacters = false
                val walker = doc.result.getArborist()
                val dataBuilder = DataTreeBuilder(opts)
                walker.getTree(doc.getAdapter(dataBuilder))
                val dtree = dataBuilder.tree

                val command = parseJson(dtree.asJSON())
                when (command["name"]) {
                    null -> {
                        println("Parse failed: ${line}")
                        continue
                    }
                    "breakpoint" -> doBreakpoint(command)
                    "define" -> doDefine(command)
                    "down" -> doDown(command)
                    "eval" -> doEval(command)
                    "exit" -> doExit()
                    "help" -> doHelp(command)
                    "inputs" -> doInputs()
                    "models" -> doModels(command)
                    "namespace" -> doNamespace(command)
                    "options" -> doOptions()
                    "run" -> {
                        stepping = false
                        return
                    }
                    "set" -> doSet(command)
                    "stack" -> doStack(command)
                    "step" -> {
                        stepping = true
                        return
                    }
                    "subpipeline" -> doSubpipeline()
                    "up" -> doUp(command)
                    else -> {
                        println("Unexpected command: ${command["name"]}")
                        continue
                    }
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is EndOfFileException -> Unit
                is XProcException -> throw ex
                else -> throw XProcError.xiImpossible("Unexpected exception from jline: ${ex}").exception(ex)
            }
        }
    }

    override fun sendDocument(from: Pair<String, String>, to: Pair<String, String>, document: XProcDocument): XProcDocument {
        for (bp in breakpoints[from.first] ?: emptyList()) {
            if (bp is OutputBreakpoint && bp.port == from.second) {
                if (document.value is XdmItem) {
                    val ebv = evalExpression(bp.expr, document.value as XdmItem, true)
                    if ((ebv.underlyingValue as BooleanValue).booleanValue) {
                        if (bp.expr == "true()") {
                            println("Output from ${from.first} on ${from.second}")
                        } else {
                            println("Output from ${from.first} on ${from.second} satisfies ${bp.expr}")
                        }

                        stack = stacks[Thread.currentThread().id] ?: Stack()
                        curFrame = stack.peek()
                        frameNumber = stack.size - 1

                        val varname = QName(curFrame.cx, NsCx.namespace.toString(), "document")
                        localVariables[varname] = document.value
                        cli(curFrame.step)

                        val newValue = localVariables.remove(varname)!!

                        if (document.value !== localVariables[varname]) {
                            return document.with(newValue)
                        }

                        return document
                    }
                }
            }
        }

        return document
    }

    private fun parseJson(json: String): Map<String,String> {
        val stepConfig = curFrame.step.stepConfig
        val a = QName("a")

        val compiler = stepConfig.newXPathCompiler()
        compiler.declareVariable(a)
        val exec = compiler.compile("parse-json(\$a)")
        val selector = exec.load()
        selector.setVariable(a, XdmAtomicValue(json))
        val value = (selector.evaluate() as XdmMap).get("command") as XdmMap
        val command = stepConfig.asMap(stepConfig.forceQNameKeys(value))
        val map = mutableMapOf<String, String>()
        for ((key, value) in command) {
            map[key.localName] = value.underlyingValue.stringValue
        }
        return map
    }

    private fun doDefine(command: Map<String,String>) {
        val inscopeNS = combinedNamespaces()

        val varname = try {
            curFrame.step.stepConfig.parseQName(command["varname"]!!, inscopeNS)
        } catch (ex: Exception) {
            println(ex.message ?: "Cannot parse name: ${command["varname"]}: ${ex.message ?: ""}")
            return
        }

        if (varname in localVariables) {
            println("You cannot define a variable that already exists; use set")
            return
        }

        val value = evalExpression(command["expr"]!!)
        localVariables[varname] = value
    }

    private fun doEval(command: Map<String,String>) {
        val value = evalExpression(command["expr"]!!)
        println(value)
    }

    private fun doSet(command: Map<String,String>) {
        val inscopeNS = combinedNamespaces()

        val varname = try {
            curFrame.step.stepConfig.parseQName(command["varname"]!!, inscopeNS)
        } catch (ex: Exception) {
            println(ex.message ?: "Cannot parse name: ${command["varname"]}: ${ex.message ?: ""}")
            return
        }

        val value = evalExpression(command["expr"]!!)
        if (varname in localVariables) {
            if (varname != NsCx.document) {
                println("Setting local variable \$${command["varname"]}...")
            }
            localVariables[varname] = value
            return
        }

        if (varname in curFrame.options) {
            println("Setting step option \$${command["varname"]}...")
            val lazyValue = LazyValue(curFrame.step.stepConfig, value, curFrame.step.stepConfig)
            curFrame.options[varname] = lazyValue
            return
        }

        println("No local variable or step option named \$${command["varname"]}...")
    }

    private fun combinedNamespaces(): Map<String,NamespaceUri> {
        // Make the namespaces and variables uniform
        var inscopeNs = mutableMapOf<String,NamespaceUri>()

        for ((prefix, uri) in curFrame.step.stepConfig.inscopeNamespaces) {
            inscopeNs[prefix] = uri
        }
        inscopeNs[curFrame.cx] = NsCx.namespace
        for ((prefix, uri) in localNamespaces) {
            inscopeNs[prefix] = uri
        }

        return inscopeNs
    }

    private fun combinedVariables(): Map<QName, XdmValue> {
        val variables = mutableMapOf<QName, XdmValue>()

        for ((name, value) in curFrame.options) {
            variables[name] = value.value
        }

        val cx_inputs = QName(NsCx.namespace, "${curFrame.cx}:input")
        var xdmMap = XdmMap()
        for ((port, documents) in curFrame.inputs) {
            var value: XdmValue = XdmEmptySequence.getInstance()
            for (doc in documents) {
                value = value.append(doc.value)
            }
            xdmMap = xdmMap.put(XdmAtomicValue(port), value)
        }
        variables[cx_inputs] = xdmMap

        for ((name, value) in localVariables) {
            variables[name] = value
        }

        return variables
    }

    private fun evalExpression(expr: String, context: XdmItem? = null, ebv: Boolean = false): XdmValue {
        val cx_inputs = QName(NsCx.namespace, "${curFrame.cx}:input")

        // Make the namespaces and variables uniform
        var inscopeNs = combinedNamespaces()
        val variables = combinedVariables()

        try {
            val stepConfig = curFrame.step.stepConfig
            val compiler = stepConfig.newXPathCompiler()
            for ((prefix, uri) in inscopeNs) {
                compiler.declareNamespace(prefix, uri.toString())
            }
            for ((name, _) in variables) {
                compiler.declareVariable(name)
            }

            if (curFrame.step.stepConfig.baseUri != null) {
                compiler.baseURI = curFrame.step.stepConfig.baseUri
            }

            val exec = compiler.compile(expr)
            val selector = exec.load()
            for ((name, value) in variables) {
                selector.setVariable(name, value)
            }

            if (context != null) {
                selector.contextItem = context
            }

            val value = if (ebv) {
                XdmAtomicValue(selector.effectiveBooleanValue())
            } else {
                selector.evaluate()
            }

            return value
        } catch (ex: Exception) {
            println(ex.message ?: "Exception while evaluating: ${expr}")
            if (ebv) {
                return XdmAtomicValue(false)
            }
            return XdmEmptySequence.getInstance()
        }
    }

    private fun evalBreakpoint(bp: Breakpoint): Boolean {
        when (bp) {
            is UnconditionalBreakpoint -> return true
            is OutputBreakpoint -> return false
            is InputBreakpoint -> return evalInputBreakpoint(bp)
            else -> Unit
        }

        return (evalExpression(bp.expr, null, true).underlyingValue as BooleanValue).booleanValue
    }

    private fun evalInputBreakpoint(bp: InputBreakpoint): Boolean {
        if (!curFrame.inputs.containsKey(bp.port)) {
            return false
        }

        val docs = curFrame.inputs[bp.port]!!
        for (doc in docs) {
            if (doc.value is XdmItem) {
                val ebv = (evalExpression(bp.expr, doc.value as XdmItem, true).underlyingValue as BooleanValue).booleanValue
                if (ebv) {
                    if (bp.expr == "true()") {
                        println("Input to ${bp.id} on ${bp.port}")
                    } else {
                        println("Input to ${bp.id} on ${bp.port} satisfies ${bp.expr}")
                    }
                    return true
                }
            }
        }

        return false
    }

    fun doOptions() {
        if (curFrame.options.isNotEmpty()) {
            println("Step options:")
            for ((name, _) in curFrame.options) {
                println("  \$${name}")
            }
        }
        if (localVariables.isNotEmpty()) {
            println("Local variables:")
            for ((name, _) in localVariables) {
                println("  \$${name}")
            }
        }
    }

    fun doInputs() {
        for ((port, value) in curFrame.inputs) {
            println("${port}: ${value.size} documents")
        }
    }

    fun doModels(command: Map<String,String>) {
        val id = command["id"]
        if (id == null) {
            val start = runtime.start.id
            for ((key, value) in runtime.pipelines) {
                print(if (start == key.id) "*" else " ")
                println("${key.id} ${value.model}")
            }
            return
        }

        var model: SubpipelineModel? = null
        for ((key, value) in runtime.pipelines) {
            if (id == key.id) {
                model = value
                break
            }
        }

        if (model == null) {
            println("No model: ${id}")
            return
        }

        for (step in model.model.children) {
            println("${step.id} ${step}")
        }
    }

    fun doStack(command: Map<String,String>) {
        val frame = command["frame"]?.toInt() ?: frameNumber
        if (frame < 0 || frame > stack.size) {
            println("Cannot move to stack frame ${frame}")
            return
        }

        frameNumber = frame
        curFrame = stack[frameNumber]

        for (index in 0 ..< stack.size) {
            val frame = stack[index]
            println("${if (frameNumber == index) "*" else " "}[${index}] ${frame.step.name}")
        }
    }

    fun doSubpipeline() {
        val step = curFrame.step
        if (step is CompoundStep) {
            for (step in step.runnables) {
                println("${step.id} / ${step.name}")
            }
        } else {
            println("This step has no subpipeline")
        }
    }

    fun doUp(command: Map<String,String>) {
        val offset = command["frames"]?.toInt() ?: 1
        frameNumber = Math.max(frameNumber - offset, 0)
        curFrame = stack[frameNumber]
    }

    fun doDown(command: Map<String,String>) {
        val offset = command["frames"]?.toInt() ?: 1
        frameNumber = Math.min(frameNumber + offset, stack.size - 1)
        curFrame = stack[frameNumber]
    }

    fun doBreakpoint(command: Map<String,String>) {
        if ("id" !in command) {
            if (breakpoints.isEmpty()) {
                println("No breakpoints")
            } else {
                println("Active breakpoints:")
                for ((id, list) in breakpoints) {
                    if (list.size == 1) {
                        println("${id}: ${list.first()}")
                    } else {
                        println("${id}:")
                        for (bp in list) {
                            println("  ${bp}")
                        }
                    }
                }
            }
            return
        }

        val stepId = command["id"]!!
        val expr = command["when"] ?: "true()"

        val list = breakpoints[stepId] ?: mutableListOf()
        breakpoints[stepId] = list

        if ("input" in command) {
            list.add(InputBreakpoint(stepId, command["input"]!!, expr))
        } else if ("output" in command) {
            list.add(OutputBreakpoint(stepId, command["output"]!!, expr))
        } else {
            if ("when" in command) {
                list.add(ConditionalBreakpoint(stepId, expr))
            } else {
                list.add(UnconditionalBreakpoint(stepId))
            }
        }
    }

    fun doHelp(command: Map<String,String>) {
        if (help.isEmpty()) {
            val text = CliDebugger::class.java.getResource("/com/xmlcalabash/debugger.txt")
                ?.readText(StandardCharsets.UTF_8)
            if (text == null) {
                println("Help is not available.")
                return
            }
            help.addAll(text.split("\n").map { it.trimEnd() })
        }

        val topic = command["topic"]
        if (topic == null) {
            for (line in help.filter { it != "" && !it.startsWith(" ") }) {
                println(line)
            }
            return
        }

        var doPrint = false
        for (line in help) {
            if (doPrint && line.startsWith("*")) {
                return
            }

            if (line.startsWith("* ${topic} ") || line.startsWith("* ${topic}:")) {
                doPrint = true
            }

            if (doPrint) {
                println(line)
            }
        }

    }

    fun doExit() {
        throw XProcError.xiAbortDebugger().exception()
    }

    fun doNamespace(command: Map<String,String>) {
        val prefix = command["prefix"]

        if (prefix == null) {
            val inscopeNs = mutableMapOf<String, NamespaceUri>()
            inscopeNs.putAll(curFrame.step.stepConfig.inscopeNamespaces)
            inscopeNs[curFrame.cx] = NsCx.namespace
            for ((prefix, uri) in localNamespaces) {
                inscopeNs[prefix] = uri
            }
            println("xmlns = \"\"")
            for ((prefix, uri) in inscopeNs) {
                if (prefix != "xml") {
                    println("xmlns:${prefix} = \"${uri}\"")
                }
            }
            return
        }

        if (prefix == "xml" || prefix == "xmlns") {
            println("You cannot change the ${prefix} prefix.")
            return
        }

        val uri = command["uri"]
        if (uri == null) {
            localNamespaces.remove(prefix)
        } else {
            if (uri == NsXmlns.namespace.toString()) {
                println("You cannot change the ${uri} URI.")
            } else {
                localNamespaces[prefix] = NamespaceUri.of(uri)
            }
        }
    }

    private fun init() {
        val invisibleXml = InvisibleXml()
        val stream = CliDebugger::class.java.getResourceAsStream("/com/xmlcalabash/debugger.ixml")
        parser = invisibleXml.getParser(stream, "https://xmlcalabash.com/grammar/debugger.ixml")
    }

    inner class DebuggerCommand(val command: String, val desc: String, val aliases: List<String>, val args: List<String>, val process: () -> Boolean) {
    }

    open inner class Breakpoint(val id: String, val expr: String) {
    }

    inner class UnconditionalBreakpoint(id: String): Breakpoint(id, "true()") {
        override fun toString(): String {
            return "on step ${id}"
        }
    }

    inner class ConditionalBreakpoint(id: String, expr: String): Breakpoint(id, expr) {
        override fun toString(): String {
            return "on step ${id} when ${expr}"
        }
    }

    inner class InputBreakpoint(id: String, val port: String, expr: String): Breakpoint(id, expr) {
        override fun toString(): String {
            return "on step ${id} input ${port}${if (expr != "true()") " when $expr" else ""}"
        }
    }

    inner class OutputBreakpoint(id: String, val port: String, expr: String): Breakpoint(id, expr) {
        override fun toString(): String {
            return "on step ${id} input ${port}${if (expr != "true()") " when $expr" else ""}"
        }
    }

    inner class StackFrame(val step: AbstractStep) {
        val options: MutableMap<QName, LazyValue>
        val inputs: MutableMap<String, MutableList<XProcDocument>>
        val cx: String

        init {
            var _cx = "cx"
            var suffix = 0
            while (step.stepConfig.inscopeNamespaces[_cx] != null && step.stepConfig.inscopeNamespaces[_cx] != NsCx.namespace) {
                suffix++
                _cx = "cx${suffix}"
            }
            cx = _cx

            when (step) {
                is AtomicStep -> {
                    val impl = step.implementation as AbstractAtomicStep
                    inputs = impl._queues
                    options = impl._options
                }
                is CompoundStepHead -> {
                    inputs = step._cache
                    options = mutableMapOf() // FIXME: this is wrong
                }
                is CompoundStepFoot -> {
                    inputs = mutableMapOf()
                    options = mutableMapOf()
                }
                is PipelineStep, is CompoundStep -> {
                    inputs = mutableMapOf()
                    options = mutableMapOf()
                }
                else -> {
                    inputs = mutableMapOf()
                    options = mutableMapOf()
                    println("Unexpected step type: ${step}")
                }
            }
        }
    }
}