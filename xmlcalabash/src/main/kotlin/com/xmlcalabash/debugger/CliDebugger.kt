package com.xmlcalabash.debugger

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.graph.SubpipelineModel
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
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeefilter.InvisibleXmlParser
import org.nineml.coffeefilter.ParserOptions
import org.nineml.coffeefilter.trees.DataTreeBuilder
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.util.*

class CliDebugger(val runtime: XProcRuntime): Monitor {
    val terminal = TerminalBuilder.terminal()
    var reader: LineReader? = null
    val prompt = "> "
    val printer = runtime.environment.xmlCalabash.xmlCalabashConfig.messagePrinter

    var parser: InvisibleXmlParser? = null
    val stacks = mutableMapOf<Long, Stack<StackFrame>>()
    val breakpoints = mutableMapOf<String, MutableList<Breakpoint>>()
    var breakNext: Breakpoint? = null
    val catchpoints = mutableListOf<Catchpoint>()
    var stepping = true
    var stopOnEnd = false
    var help = mutableListOf<String>()

    val localNamespaces = mutableMapOf<String, NamespaceUri>()
    val localVariables = mutableMapOf<QName, XdmValue>()
    var localBaseUri: URI? = null

    lateinit var curFrame: StackFrame
    var stack: Stack<StackFrame> = Stack()
    var frameNumber = -1

    val stepList = mutableListOf<String>()

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
        if (!stopHere && breakNext?.id == step.id) {
            stopHere = true
        }

        if (stopHere) {
            breakNext = null
            localBaseUri = curFrame.step.stepConfig.baseUri
            cli(step, true)
        }
    }

    override fun endStep(step: AbstractStep) {
        frameNumber = stack.size - 1

        if (stopOnEnd) {
            stopOnEnd = false
            cli(step, false)
        }

        stack.pop()
    }

    override fun abortStep(step: AbstractStep, ex: Exception) {
        frameNumber = stack.size - 1

        val code = if (ex is XProcException) {
            ex.error.code
        } else {
            null
        }

        for (point in catchpoints) {
            val catchCode = if (point.code != null) {
                try {
                    step.stepConfig.parseQName(point.code)
                } catch (_: Exception) {
                    printer.println("Catch failed to parse ${point.code}")
                    continue
                }
            } else {
                null
            }

            if ((point.id == null || point.id == step.id)
                && (catchCode == null || catchCode == code)) {
                if (point.id == null) {
                    if (code == null) {
                        printer.println("Debugger caught error")
                    } else {
                        printer.println("Debugger caught ${code}")
                    }
                } else {
                    if (code == null) {
                        printer.println("Debugger caught error on ${point.id}")
                    } else {
                        printer.println("Debugger caught ${code} on ${point.id}")
                    }
                }

                stopOnEnd = false
                cli(step, false)
            }
        }

        if (stopOnEnd) {
            stopOnEnd = false
            cli(step, false)
        }

        stack.pop()
    }

    private fun cli(step: AbstractStep, start: Boolean) {
        if (reader == null) {
            reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build()
        }


        if (start) {
            printer.println("Debugger at ${step.id}")
        } else {
            printer.println("Debugger at end of ${step.id}")
        }

        if (curFrame.cx != "cx") {
            printer.println("xmlns:${curFrame.cx} = ${NsCx.namespace}")
        }

        try {
            while (true) {
                val line = reader!!.readLine(prompt).trim()
                if (line == "") {
                    continue
                }

                val doc = parser!!.parse(line)
                if (!doc.succeeded()) {
                    printer.println("Syntax error: ${line}")
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
                        printer.println("Parse failed: ${line}")
                        continue
                    }
                    "base-uri" -> doBaseUri(command)
                    "breakpoint" -> doBreakpoint(command)
                    "catch" -> doCatchpoint(command)
                    "define" -> doDefine(command)
                    "down" -> doDown(command)
                    "eval" -> doEval(command)
                    "exit" -> doExit()
                    "help" -> doHelp(command)
                    "inputs" -> doInputs()
                    "models" -> doModels(command)
                    "namespace" -> doNamespace(command)
                    "next" -> {
                        doNext(command)
                        if (breakNext != null) {
                            return
                        }
                    }
                    "options" -> doOptions()
                    "run" -> {
                        doRun(command)
                        return
                    }
                    "set" -> doSet(command)
                    "stack" -> doStack(command)
                    "step" -> {
                        doStep(command)
                        return
                    }
                    "subpipeline" -> doSubpipeline(command)
                    "up" -> doUp(command)
                    else -> {
                        printer.println("Unexpected command: ${command["name"]}")
                        continue
                    }
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is EndOfFileException -> Unit
                is UserInterruptException -> doExit()
                is XProcException -> throw ex
                else -> throw XProcError.xiImpossible("Unexpected exception from jline: ${ex}").exception(ex)
            }
        }
    }

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        for (bp in breakpoints[from.first.id] ?: emptyList()) {
            if (bp is OutputBreakpoint && bp.port == from.second) {
                if (document.value is XdmItem) {
                    val ebv = evalExpression(bp.expr, document.value as XdmItem, true)
                    if ((ebv.underlyingValue as BooleanValue).booleanValue) {
                        if (bp.expr == "true()") {
                            printer.println("Output from ${from.first} on ${from.second}")
                        } else {
                            printer.println("Output from ${from.first} on ${from.second} satisfies ${bp.expr}")
                        }

                        stack = stacks[Thread.currentThread().id] ?: Stack()
                        curFrame = stack.peek()
                        frameNumber = stack.size - 1

                        val varname = QName(curFrame.cx, NsCx.namespace.toString(), "document")
                        localVariables[varname] = document.value
                        cli(curFrame.step, true)

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
            printer.println(ex.message ?: "Cannot parse name: ${command["varname"]}: ${ex.message ?: ""}")
            return
        }

        if (varname in localVariables) {
            printer.println("You cannot define a variable that already exists; use set")
            return
        }

        val value = evalExpression(command["expr"]!!)
        localVariables[varname] = value
    }

    private fun doEval(command: Map<String,String>) {
        val value = evalExpression(command["expr"]!!)
        prettyPrint(value)
    }

    private fun prettyPrint(value: XdmValue) {
        if (value !is XdmMap && value !is XdmArray) {
            printer.println("${value}")
            return
        }

        try {
            val var_a = QName("a")
            val stepConfig = curFrame.step.stepConfig
            val compiler = stepConfig.newXPathCompiler()
            compiler.declareVariable(var_a)
            val options = "map{'method':'json','indent':true(),'escape-solidus':false()}"
            val exec = compiler.compile("serialize(\$a, ${options})")
            val selector = exec.load()
            selector.setVariable(var_a, value)
            val value = selector.evaluate()
            printer.println("${value}")
        } catch (ex: Exception) {
            printer.println(ex.message ?: "Exception while formatting ${value}")
        }
    }

    private fun doSet(command: Map<String,String>) {
        val inscopeNS = combinedNamespaces()

        val varname = try {
            curFrame.step.stepConfig.parseQName(command["varname"]!!, inscopeNS)
        } catch (ex: Exception) {
            printer.println(ex.message ?: "Cannot parse name: ${command["varname"]}: ${ex.message ?: ""}")
            return
        }

        val value = evalExpression(command["expr"]!!)
        if (varname in localVariables) {
            if (varname != NsCx.document) {
                printer.println("Setting local variable \$${command["varname"]}...")
            }
            localVariables[varname] = value
            return
        }

        if (varname in curFrame.options) {
            printer.println("Setting step option \$${command["varname"]}...")
            val lazyValue = LazyValue(XProcDocument.ofValue(value, curFrame.step.stepConfig), curFrame.step.stepConfig)
            curFrame.options[varname] = lazyValue
            return
        }

        printer.println("No local variable or step option named \$${command["varname"]}...")
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
            val execContext = curFrame.step.stepConfig.environment.newExecutionContext(curFrame.step.stepConfig)
            for ((port, list) in curFrame.inputs) {
                for (doc in list) {
                    execContext.addProperties(doc)
                }
            }

            val stepConfig = curFrame.step.stepConfig
            val compiler = stepConfig.newXPathCompiler()
            for ((prefix, uri) in inscopeNs) {
                compiler.declareNamespace(prefix, uri.toString())
            }
            for ((name, _) in variables) {
                compiler.declareVariable(name)
            }

            if (localBaseUri != null) {
                compiler.baseURI = localBaseUri
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
            printer.println(ex.message ?: "Exception while evaluating: ${expr}")
            if (ebv) {
                return XdmAtomicValue(false)
            }
            return XdmEmptySequence.getInstance()
        } finally {
            curFrame.step.stepConfig.environment.releaseExecutionContext()
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
                        printer.println("Input to ${bp.id} on ${bp.port}")
                    } else {
                        printer.println("Input to ${bp.id} on ${bp.port} satisfies ${bp.expr}")
                    }
                    return true
                }
            }
        }

        return false
    }

    fun doOptions() {
        if (curFrame.options.isNotEmpty()) {
            printer.println("Step options:")
            for ((name, _) in curFrame.options) {
                printer.println("  \$${name}")
            }
        }
        if (localVariables.isNotEmpty()) {
            printer.println("Local variables:")
            for ((name, _) in localVariables) {
                printer.println("  \$${name}")
            }
        }
    }

    fun doInputs() {
        for ((port, value) in curFrame.inputs) {
            printer.println("${port}: ${value.size} documents")
        }
    }

    fun doModels(command: Map<String,String>) {
        val id = command["id"]
        if (id == null) {
            val start = runtime.start.id
            var maxlen = 0
            for ((_, value) in runtime.pipelines) {
                if (value.id.length > maxlen) {
                    maxlen = value.id.length
                }
            }

            for ((key, value) in runtime.pipelines) {
                printer.print(if (start == key.id) "*" else " ")
                printer.println("${value.id.padEnd(maxlen)} ... ${value.model}")
            }

            return
        }

        var model: SubpipelineModel? = null
        for ((_, value) in runtime.pipelines) {
            if (id == value.id) {
                model = value
                break
            }
        }

        if (model == null) {
            printer.println("No model: ${id}")
            return
        }

        var maxlen = 0
        for (step in model.model.children) {
            if (step.id.length > maxlen) {
                maxlen = step.id.length
            }
        }

        for (step in model.model.children) {
            printer.println("${step.id.padEnd(maxlen)} ... ${step}")
        }

        val graph = runtime.graphList.first { it == model.model.graph }
        showList("Steps: ", graph.models.filter { it !is SubpipelineModel }.map { it.id })
    }

    private fun showList(title: String, list: List<String>) {
        val width = if (terminal.width > 12) {
            terminal.width - 5 // leave a little space at the end of the line
        } else {
            60 // some random default that's not too wide for most terminals
        }

        val sb = StringBuilder()
        var first = true
        sb.append(title)

        for (item in list) {
            if (!first) {
                sb.append(", ")
            }

            if (sb.length + item.length > width) {
                printer.println(sb.toString())
                sb.clear()
                sb.append("".padEnd(title.length))
                first = true
            }

            first = false

            sb.append(item)
        }

        if (sb.toString().isNotBlank()) {
            printer.println(sb.toString())
        }
    }

    fun doStack(command: Map<String,String>) {
        val frame = command["frame"]?.toInt() ?: frameNumber
        if (frame < 0 || frame > stack.size) {
            printer.println("Cannot move to stack frame ${frame}")
            return
        }

        frameNumber = frame
        curFrame = stack[frameNumber]

        for (index in 0 ..< stack.size) {
            val frame = stack[index]
            printer.println("${if (frameNumber == index) "*" else " "}[${index}] ${frame.step.name}")
        }
    }

    fun doRun(command: Map<String,String>) {
        stepping = false
        stopOnEnd = false
    }

    fun doStep(command: Map<String,String>) {
        stepping = true
        stopOnEnd = command["end"] != null
    }

    fun doNext(command: Map<String,String>) {
        breakNext = null
        val next = nextStep(stack.size - 1)
        if (next != null) {
            breakNext = Breakpoint(next.id, "true()")
            stepping = false
            stopOnEnd = false
        }
    }

    private fun nextStep(frameNo: Int): AbstractStep? {
        val frame = stack[frameNo]
        val upNo = frameNo - 1
        val up = if (upNo >= 0) {
            stack[upNo]
        } else {
            null
        }

        val curStep = frame.step

        when (curStep) {
            is CompoundStepHead -> {
                if (up != null) {
                    return up.subpipeline[0]
                }
            }
            is CompoundStepFoot -> {
                if (frameNo > 0) {
                    return nextStep(frameNo - 1)
                }
            }
            else -> {
                if (up == null && curStep is PipelineStep) {
                    return curStep.head
                }

                var nextStep: AbstractStep? = null
                if (up != null) {
                    for ((cidx, child) in up.subpipeline.withIndex()) {
                        if (child == curStep) {
                            if (cidx + 1 < up.subpipeline.size) {
                                nextStep = up.subpipeline[cidx + 1]
                            } else {
                                nextStep = (up.step as CompoundStep).foot
                            }
                        }
                    }
                }

                if (nextStep != null) {
                    return nextStep
                }

                if (frameNo > 0) {
                    return nextStep(frameNo - 1)
                }
            }
        }

        return null
    }

    fun doSubpipeline(command: Map<String,String>) {
        var step = curFrame.step
        if (step !is CompoundStep) {
            printer.println("This step has no subpipeline")
            return
        }

        val target = command["id"]
        if (target != null) {
            var found = false
            for (substep in step.runnables) {
                if (target == substep.id) {
                    found = true
                    step = substep
                    break
                }
            }
            if (!found) {
                printer.println("No subpipeline: ${target}")
                return
            }
        }

        if (step is CompoundStep) {
            var maxlen = 0
            if (step.runnables.isEmpty()) {
                for ((model, _) in step.runnableProviders) {
                    if (model.id.length > maxlen) {
                        maxlen = model.id.length
                    }
                }
                for ((model, _) in step.runnableProviders) {
                    printer.println("${model.id.padEnd(maxlen)} ... ${model.type}")
                }
            } else {
                for (step in step.runnables) {
                    if (step.id.length > maxlen) {
                        maxlen = step.id.length
                    }
                }
                for (step in step.runnables) {
                    printer.println("${step.id.padEnd(maxlen)} ... ${step.type}")
                }
            }
        } else {
            printer.println("Step has no subpipeline")
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

    fun doBaseUri(command: Map<String,String>) {
        if ("uri" in command) {
            try {
                localBaseUri = URI(command["uri"]!!)
            } catch (ex: URISyntaxException) {
                printer.println("Not a valid URI: ${command["uri"]!!}")
            }
            return
        }
        if (localBaseUri != null) {
            printer.println("${localBaseUri}")
        } else {
            printer.println("The base URI is undefined.")
        }
    }

    fun doBreakpoint(command: Map<String,String>) {
        if ("id" !in command) {
            if (breakpoints.isEmpty()) {
                printer.println("No breakpoints")
            } else {
                printer.println("Active breakpoints:")
                for ((id, list) in breakpoints) {
                    if (list.size == 1) {
                        printer.println("${id}: ${list.first()}")
                    } else {
                        printer.println("${id}:")
                        for (bp in list) {
                            printer.println("  ${bp}")
                        }
                    }
                }
            }
            return
        }

        val stepId = command["id"]!!

        if (stepId !in stepList) {
            printer.println("Error: \"${stepId}\" does not identify a step")
            val options = stepList.filter { it.startsWith(stepId.substring(0,1)) }
            if (options.isNotEmpty()) {
                showList("Similar: ", options)
            }
            return
        }

        if ("clear" in command) {
            if (stepId in breakpoints) {
                breakpoints.remove(stepId)
            } else {
                printer.println("No breakpoints set on ${stepId}")
            }
            return
        }

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

    fun doCatchpoint(command: Map<String,String>) {
        if ("id" !in command && "code" !in command) {
            if (catchpoints.isEmpty()) {
                printer.println("No catch points")
            } else {
                printer.println("Active catch points:")
                for (point in catchpoints) {
                    if (point.id == null && point.code == null) {
                        printer.println("  Catch any error on any step")
                    } else if (point.id != null) {
                        if (point.code == null) {
                            printer.println("  Catch any error on ${point.id}")
                        } else {
                            printer.println("  Catch ${point.code} on ${point.id}")
                        }
                    } else {
                        printer.println("  Catch ${point.code} on any step")
                    }
                }
            }
            return
        }

        val stepId = command["id"]

        val newPoint = if (stepId == "*") {
            Catchpoint(null, command["code"])
        } else {
            Catchpoint(stepId, command["code"])
        }

        if (catchpoints.any { it.id == newPoint.id && it.code == newPoint.code }) {
            return
        }

        catchpoints.add(newPoint)
    }

    fun doHelp(command: Map<String,String>) {
        if (help.isEmpty()) {
            val text = CliDebugger::class.java.getResource("/com/xmlcalabash/debugger.txt")
                ?.readText(StandardCharsets.UTF_8)
            if (text == null) {
                printer.println("Help is not available.")
                return
            }
            help.addAll(text.split("\n").map { it.trimEnd() })
        }

        val topic = command["topic"]
        if (topic == null) {
            for (line in help.filter { it != "" && !it.startsWith(" ") }) {
                printer.println(line)
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
                printer.println(line)
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
            printer.println("xmlns = \"\"")
            for ((prefix, uri) in inscopeNs) {
                if (prefix != "xml") {
                    printer.println("xmlns:${prefix} = \"${uri}\"")
                }
            }
            return
        }

        if (prefix == "xml" || prefix == "xmlns") {
            printer.println("You cannot change the ${prefix} prefix.")
            return
        }

        val uri = command["uri"]
        if (uri == null) {
            localNamespaces.remove(prefix)
        } else {
            if (uri == NsXmlns.namespace.toString()) {
                printer.println("You cannot change the ${uri} URI.")
            } else {
                localNamespaces[prefix] = NamespaceUri.of(uri)
            }
        }
    }

    private fun init() {
        val invisibleXml = InvisibleXml()
        val stream = CliDebugger::class.java.getResourceAsStream("/com/xmlcalabash/debugger.ixml")
        parser = invisibleXml.getParser(stream, "https://xmlcalabash.com/grammar/debugger.ixml")

        for (graph in runtime.graphList) {
            for (model in graph.models) {
                if (model !is SubpipelineModel) {
                    stepList.add(model.id)
                }
            }
        }
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

    open inner class Catchpoint(val id: String?, val code: String?) {
    }

    inner class StackFrame(val step: AbstractStep) {
        val options: MutableMap<QName, LazyValue>
        val inputs: MutableMap<String, MutableList<XProcDocument>>
        val subpipeline = mutableListOf<AbstractStep>()
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
                    options = mutableMapOf()
                    options.putAll(impl._options)
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
                    subpipeline.addAll(step.runnables)
                    inputs = mutableMapOf()
                    options = mutableMapOf()
                    for ((name, value) in step.staticOptions) {
                        val svalue = value.staticValue.evaluate(value.stepConfig)
                        options.put(name, LazyValue(XProcDocument.ofValue(svalue, value.stepConfig), value.stepConfig))
                    }
                }
                else -> {
                    inputs = mutableMapOf()
                    options = mutableMapOf()
                    printer.println("Unexpected step type: ${step}")
                }
            }
        }
    }
}
