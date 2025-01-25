package com.xmlcalabash.testdriver

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.PipelineContext
import com.xmlcalabash.util.*
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.expr.parser.ExpressionTool
import net.sf.saxon.expr.parser.ExpressionVisitor
import net.sf.saxon.expr.parser.RoleDiagnostic
import net.sf.saxon.expr.parser.Token
import net.sf.saxon.om.*
import net.sf.saxon.s9api.*
import net.sf.saxon.type.BuiltInAtomicType
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Supplier
import javax.xml.transform.sax.SAXSource

class TestCase(val suite: TestSuite, val testFile: File) {
    companion object {
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val CODE = QName("code")
        val EXPECTED = QName("expected")
        val FEATURES = QName("features")
        val SRC = QName("src")
        val PORT = QName("port")
        val NAME = QName("name")
        val STATIC = QName("static")
        val SELECT = QName("select")
        val PATH = QName("path")
        val LAST_MODIFIED = QName("last-modified")
        val READABLE = QName("readable")
        val WRITABLE = QName("writable")
        val HIDDEN = QName("hidden")

        val UNSUPPORTED_FEATURES = listOf("xslt-1", "xquery_1_0")
    }

    val config = suite.xmlCalabash
    val builder = config.newPipelineBuilder()
    var testConfig = builder.stepConfig.copy()
    //var graphviz: File? = config.xmlCalabashConfig.graphviz
    var graphviz: File? = File("/opt/homebrew/bin/dot") // FIXME:

    var loaded = false

    var status = TestStatus("NOTRUN")
    var singleTest = false
    var expected = "UNKNOWN"
    val errorCodes = mutableListOf<QName>()
    val features = mutableListOf<String>()
    var pipelineXml: XdmNode? = null
    val inputs = mutableMapOf<String,MutableList<XdmNode>>()
    val options = mutableMapOf<QName,XProcDocument>()
    val staticOptions = mutableMapOf<QName,XProcDocument>()
    var schematron: XdmNode? = null
    var fileEnvironment: FileEnvironment? = null
    var elapsedSeconds: Double = -1.0
    var stderrOutput = ""
    var stdoutOutput = ""
    var messages: XdmNode? = null
    val catalogs = mutableListOf<String>()

    var stdoutBais: ByteArrayOutputStream? = null
    var stderrBais: ByteArrayOutputStream? = null
    var origOut: PrintStream? = null
    var origErr: PrintStream? = null

    fun load() {
        loaded = true
        val builder = config.saxonConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val testXml = builder.build(testFile)
        loadTest(rootElement(testXml))
    }

    fun run() {
        if (features.isNotEmpty()) {
            for (feature in UNSUPPORTED_FEATURES) {
                if (feature in features) {
                    skip("Unsupported feature: ${feature}")
                    return
                }
            }
        }

        val eagerEval = "eager-eval" in features

        val saveOS = Urify.osname
        val saveSep = Urify.filesep
        val saveCwd = Urify.cwd
        if (features.contains("urify-windows") || features.contains("urify-non-windows")) {
            if (isWindows) {
                if (features.contains("urify-non-windows")) {
                    Urify.mockOs("MacOS", "/", null)
                }
            } else {
                if (features.contains("urify-windows")) {
                    Urify.mockOs("Windows", "\\", null)
                }
            }
        }

        if (fileEnvironment != null) {
            setupFileEnvironment(fileEnvironment!!)
        }

        lateinit var messageReporter: BufferingMessageReporter
        try {
            println(testFile.absolutePath)

            config.commonEnvironment.eagerEvaluation = eagerEval
            val parser = config.newXProcParser(builder)
            for ((name, value) in staticOptions) {
                parser.builder.option(name, value)
            }

            val treeBuilder = SaxonTreeBuilder(testConfig)
            treeBuilder.startDocument(pipelineXml!!.baseURI)
            treeBuilder.addSubtree(pipelineXml!!)
            treeBuilder.endDocument()
            val document = treeBuilder.result

            messageReporter = BufferingMessageReporter(LoggingMessageReporter())

            val decl = parser.parse(document)
            val runtime = decl.runtime()

            for (catalog in catalogs) {
                val stream = ByteArrayInputStream(catalog.toByteArray(StandardCharsets.UTF_8))
                val source = InputSource(stream)
                source.systemId = testFile.absolutePath
                runtime.environment.documentManager.resolverConfiguration.addCatalog(testFile.toURI(), source)
            }

            messageReporter = runtime.environment.messageReporter as BufferingMessageReporter

            // Something of a hack...
            if (expected == "fail" && errorCodes.contains(NsErr.assertionFailed)) {
                (runtime.environment as PipelineContext).assertions = AssertionsLevel.ERROR
            }

            val pipeline = runtime.executable()

            if (suite.options.outputDescription != null || suite.options.outputGraph != null) {
                val description = runtime.description()
                if (suite.options.outputDescription != null) {
                    VisualizerOutput.xml(suite.xmlCalabash, description, suite.options.outputDescription!!)
                }
                if (suite.options.outputGraph != null) {
                    if (graphviz == null) {
                        logger.warn { "Cannot create SVG descriptions, graphviz is not configured" }
                    } else {
                        VisualizerOutput.svg(description, suite.options.outputGraph!!, graphviz!!.absolutePath)
                    }
                }
            }

            for ((port, docs) in inputs) {
                for (doc in docs) {
                    pipeline.input(port, XProcDocument.ofXml(doc, testConfig))
                }
            }

            for ((name, value) in options) {
                pipeline.option(name, value)
            }

            val outputReceiver = BufferingReceiver()
            pipeline.receiver = outputReceiver

            if (suite.options.report != null) {
                startIO()
            }

            val start = System.nanoTime()
            try {
                pipeline.run()
                elapsedSeconds = (System.nanoTime() - start) / 1e9
                endIO()
            } catch (e: Exception) {
                elapsedSeconds = (System.nanoTime() - start) / 1e9
                endIO()
                throw e
            }

            val result = outputReceiver.outputs["result"] ?: emptyList()

            if (singleTest) {
                for (index in 0 until result.size) {
                    if (result.size > 1) {
                        print("[${index}]: ")
                    }
                    println(result[index].value)
                }
            }

            val errors = if (schematron != null) {
                if (result.isEmpty()) {
                    listOf()
                } else {
                    validate(result[0].value)
                }
            } else {
                listOf()
            }

            if (errors.isNotEmpty()) {
                val bytes = ByteArrayOutputStream()
                val stream = PrintStream(bytes)

                for (index in 0 until result.size) {
                    stream.println("[${index}]: ")
                    result[index].serialize(stream, mapOf(Ns.indent to XdmAtomicValue(true)))
                    stream.println("----------------")
                }

                for (node in errors) {
                    stream.println(node.stringValue)
                }

                stream.close()
                stderrOutput += bytes.toString(StandardCharsets.UTF_8)

                fail(errors, messagesXml(messageReporter.messages(Verbosity.TRACE)))
            } else {
                if (expected == "pass" && result.isNotEmpty()) {
                    pass()
                } else {
                    fail(messagesXml(messageReporter.messages(Verbosity.TRACE)))
                }
            }
        } catch (ex: XProcException) {
            if (expected == "fail") {
                var ok = false
                for (code in errorCodes) {
                    ok = ok || ex.error.code == code
                }
                if (ok) {
                    pass()
                } else {
                    fail(ex.error, errorCodes, messagesXml(messageReporter.messages(Verbosity.TRACE)))
                }
            } else {
                if (singleTest) {
                    ex.printStackTrace()
                }
                fail(ex.error, messagesXml(messageReporter.messages(Verbosity.TRACE)))
            }
        } catch (ex: Exception) {
            if (singleTest) {
                ex.printStackTrace()
            }
            val error = XProcError.internal(999, ex)
            fail(error, messagesXml(messageReporter.messages(Verbosity.TRACE)))
        } catch (t: Throwable) {
            if (singleTest) {
                t.printStackTrace()
            }
            println("CRASH! ${testFile}")
            fail(messagesXml(messageReporter.messages(Verbosity.TRACE)))
        }

        if (features.contains("urify-windows") || features.contains("urify-non-windows")) {
            Urify.mockOs(saveOS, saveSep, saveCwd)
        }

        if (fileEnvironment != null) {
            teardownFileEnvironment()
        }
    }

    private fun startIO() {
        stdoutBais = ByteArrayOutputStream()
        stderrBais = ByteArrayOutputStream()
        val stdout = PrintStream(stdoutBais!!)
        val stderr = PrintStream(stderrBais!!)

        origOut = System.out
        origErr = System.err

        System.setOut(stdout)
        System.setErr(stderr)
    }

    private fun endIO() {
        if (origOut == null) {
            return
        }

        System.setOut(origOut)
        System.setErr(origErr)

        stderrOutput = stderrBais!!.toString(Charsets.UTF_8)
        if (stderrOutput != "") {
            println(stderrOutput)
        }

        stdoutOutput = stdoutBais!!.toString(Charsets.UTF_8)
        if (stdoutOutput != "") {
            println(stdoutOutput)
        }
    }

    private fun skip(reason: String) {
        suite.skip(this, reason)
    }

    private fun pass() {
        suite.pass(this)
    }

    private fun fail(msgxml: XdmNode) {
        messages = msgxml
        suite.fail(this)
    }

    private fun fail(error: XProcError, msgxml: XdmNode) {
        messages = msgxml
        suite.fail(this, error)
    }

    private fun fail(errors: List<XdmNode>, msgxml: XdmNode) {
        messages = msgxml
        suite.fail(this, errors)
    }

    private fun fail(error: XProcError, codes: List<QName>, msgxml: XdmNode) {
        messages = msgxml
        suite.fail(this, error, codes)
    }

    private fun messagesXml(messages: List<BufferingMessageReporter.Message>): XdmNode {
        val builder = SaxonTreeBuilder(suite.xmlCalabash.saxonConfig.processor)
        builder.startDocument(null)
        builder.addStartElement(NsCx.messages)
        for (message in messages) {
            builder.addStartElement(NsCx.message, attributeMap(mapOf(
                Ns.level to "${message.level}",
                Ns.message to message.message,
                Ns.date to "${message.timestamp}"
            )))
            builder.addEndElement()
        }
        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    fun attributeMap(attr: Map<QName, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(name, value))
            }
        }
        return map
    }

    private fun attributeInfo(name: QName, value: String, location: net.sf.saxon.s9api.Location? = null): AttributeInfo {
        return AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
    }

    private fun fqName(name: QName): FingerprintedQName = FingerprintedQName(name.prefix, name.namespaceUri, name.localName)

    private fun validate(doc: XdmValue): List<XdmNode> {
        val validator = SchematronImpl(testConfig)
        return validator.test(doc, schematron!!)
    }

    private fun loadTest(root: XdmNode) {
        if (root.getAttributeValue(CODE) != null) {
            for (code in root.getAttributeValue(CODE)!!.trim().split("\\s+".toRegex())) {
                errorCodes.add(QName(code, root))
            }
        }

        if (root.getAttributeValue(FEATURES) != null) {
            for (feature in root.getAttributeValue(FEATURES)!!.trim().split("\\s+".toRegex())) {
                features.add(feature)
            }
        }

        expected = root.getAttributeValue(EXPECTED)

        for (node in root.axisIterator(Axis.CHILD)) {
            if (node.nodeKind == XdmNodeKind.ELEMENT) {
                when (node.nodeName) {
                    NsT.info -> Unit
                    NsT.description -> Unit
                    NsT.pipeline -> loadPipeline(node)
                    NsT.input -> loadInput(node)
                    NsT.schematron -> loadSchematron(node)
                    NsT.option -> loadOption(node)
                    NsT.fileEnvironment -> loadFileEnvironment(node)
                    NsT.catalog -> loadCatalog(node)
                    else -> println("${testFile}: unexpected element: ${node.nodeName}")
                }
            }
        }
    }

    private fun loadPipeline(pipeline: XdmNode) {
        val xml = if (pipeline.getAttributeValue(SRC) != null) {
            val builder = testConfig.processor.newDocumentBuilder()
            builder.isLineNumbering = true
            val fn = pipeline.baseURI.resolve(pipeline.getAttributeValue(SRC))
            val xml = builder.build(SAXSource(InputSource(fn.toString())))
            rootElement(xml)
        } else {
            rootElementDocument(pipeline)
        }

        pipelineXml = xml
    }

    private fun loadInput(input: XdmNode) {
        val port = input.getAttributeValue(PORT) ?: throw RuntimeException("No port on input?")
        if (!inputs.containsKey(port)) {
            inputs[port] = mutableListOf()
        }

        val excludeNamespaces = mutableSetOf<NamespaceUri>()
        val documents = mutableListOf<XdmNode>()
        if (input.getAttributeValue(SRC) != null) {
            val builder = testConfig.processor.newDocumentBuilder()
            builder.isLineNumbering = true
            val fn = input.baseURI.resolve(input.getAttributeValue(SRC))
            val xml = builder.build(SAXSource(InputSource(fn.toString())))
            documents.add(xml)
        } else {
            excludeNamespaces.add(NsT.namespace)
            for (node in input.axisIterator(Axis.CHILD)) {
                if (node.nodeKind == XdmNodeKind.ELEMENT) {
                    documents.add(node)
                }
            }
        }

        for (doc in documents) {
            val builder = SaxonTreeBuilder(testConfig)
            builder.excludeNamespaces(excludeNamespaces)
            builder.startDocument(doc.baseURI)
            builder.addSubtree(doc)
            builder.endDocument()
            inputs[port]!!.add(builder.result)
        }
    }

    private fun loadOption(option: XdmNode) {
        val localConfig = builder.stepConfig.copy()
        localConfig.updateWith(option)
        localConfig.putNamespace(   "xs", NsXs.namespace)

        val name = option.getAttributeValue(Ns.name) ?: throw RuntimeException("No name on option?")
        val qname = localConfig.parseQName(name)
        val static = option.getAttributeValue(Ns.static) == "true"
        val asType = option.getAttributeValue(Ns.asType)

        val select = option.getAttributeValue(SELECT)

        if (select == null) {
            if (static) {
                staticOptions[qname] = XProcDocument.ofXml(rootElement(option), localConfig)
            } else {
                options[qname] = XProcDocument.ofXml(rootElement(option), localConfig)
            }
            return
        }

        val compiler = localConfig.newXPathCompiler()
        for (oname in options.keys) {
            compiler.declareVariable(oname)
        }

        val selector = compiler.compile(select).load()
        for ((oname, value) in options) {
            selector.setVariable(oname, value.value)
        }

        var result = selector.evaluate()

        if (asType != null) {
            val seqtype = localConfig.parseSequenceType(asType)

            val uncheckedExpr = ExpressionTool.make(select, compiler.underlyingStaticContext, 0, Token.EOF, null)
            val checker = localConfig.saxonConfig.configuration.getTypeChecker(false)

            val role =
                Supplier<RoleDiagnostic> {
                    RoleDiagnostic(
                        RoleDiagnostic.VARIABLE,
                        "UNKNOWN",
                        0,
                        "XXXX0000"
                    )
                }
            val visitor = ExpressionVisitor.make(compiler.underlyingStaticContext)
            checker.staticTypeCheck(uncheckedExpr, seqtype.underlyingSequenceType, role, visitor)

            result = localConfig.checkType(qname, result, seqtype, emptyList())
        }

        val doc = XProcDocument.ofJson(result, localConfig)
        if (static) {
            staticOptions[qname] = doc
        } else {
            options[qname] = doc
        }
    }

    private fun loadSchematron(schema: XdmNode) {
        val xml = if (schema.getAttributeValue(SRC) != null) {
            val builder = testConfig.processor.newDocumentBuilder()
            builder.isLineNumbering = true
            val fn = schema.baseURI.resolve(schema.getAttributeValue(SRC))
            val xml = builder.build(SAXSource(InputSource(fn.toString())))
            rootElement(xml)
        } else {
            rootElement(schema)
        }

        schematron = xml
    }

    private fun loadCatalog(catalog: XdmNode) {
        val xml = if (catalog.getAttributeValue(SRC) != null) {
            val builder = testConfig.processor.newDocumentBuilder()
            builder.isLineNumbering = true
            val fn = catalog.baseURI.resolve(catalog.getAttributeValue(SRC))
            val xml = builder.build(SAXSource(InputSource(fn.toString())))
            rootElement(xml).toString()
        } else {
            rootElement(catalog).toString()
        }

        catalogs.add(xml)
    }

    private fun loadFileEnvironment(node: XdmNode) {
        val properties = mutableListOf<TestFileProperties>()
        for (child in node.axisIterator(Axis.CHILD)) {
            if (child.nodeKind == XdmNodeKind.ELEMENT) {
                val props = when (child.nodeName) {
                    NsT.file -> loadFile(child)
                    NsT.folder -> loadFolder(child)
                    else -> {
                        println("${testFile}: unexpected in file-environment: ${child}")
                        null
                    }
                }
                if (props != null) {
                    properties.add(props)
                }
            }
        }
        fileEnvironment = FileEnvironment(properties)
    }

    private fun loadFolder(node: XdmNode): TestFolder? {
        val path = node.getAttributeValue(PATH) ?: return null
        val modified = testDate(node.getAttributeValue(LAST_MODIFIED))
        val readable = testBoolean(node.getAttributeValue(READABLE))
        val writable = testBoolean(node.getAttributeValue(WRITABLE))
        val hidden = testBoolean(node.getAttributeValue(HIDDEN))
        return TestFolder(path, modified, readable, writable, hidden)
    }

    private fun loadFile(node: XdmNode): TestFile? {
        val folder = loadFolder(node) ?: return null
        var content: String? = null
        for (child in node.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.ELEMENT -> {
                    if (child.nodeName == NsT.filetestContent) {
                        if (content != null) {
                            println("${testFile}: more than one content element?")
                        }
                        content = child.stringValue
                    }
                }
                XdmNodeKind.TEXT -> {
                    if (content != null) {
                        println("${testFile}: more than one content string?")
                    }
                    content = child.stringValue
                }
                else -> println("Unexpected content type in t:file...")
            }
        }
        return TestFile(folder.path, folder.lastModified, folder.readable, folder.writable, folder.hidden, content)
    }

    private fun testBoolean(value: String?): Boolean? {
        if (value == null) {
            return null
        }
        return value == "true"
    }

    private fun testDate(value: String?): Date? {
        if (value == null) {
            return null
        }

        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val accessor = formatter.parse(value)
        return Date.from(Instant.from(accessor))
    }

    private fun rootElement(parent: XdmNode): XdmNode {
        var elem: XdmNode? = null
        for (node in parent.axisIterator(Axis.CHILD)) {
            if (node.nodeKind == XdmNodeKind.ELEMENT) {
                if (elem == null) {
                    elem = node
                } else {
                    println("${testFile}: expected a single element child, found extra ${node.nodeName}")
                }
            }
        }
        if (elem == null) {
            throw RuntimeException("Configuration error: failed to find element in ${parent.nodeName}")
        }

        elem = S9Api.adjustBaseUri(elem, parent.baseURI)

        return elem
    }

    private fun rootElementDocument(parent: XdmNode): XdmNode {
        val elem = rootElement(parent)
        val builder = SaxonTreeBuilder(testConfig)
        builder.excludeNamespaces(setOf(NsT.namespace))
        builder.startDocument(elem.baseURI)
        try {
            builder.addSubtree(elem)
        } catch (ex: IllegalStateException) {
            // There are some tests in the test suite that can't be interpreted.
            // Just return them now, unfiltered, and assume the exception will
            // be caught later in the right place.
            return elem
        }
        builder.endDocument()
        return rootElement(builder.result)
    }

    private fun pipelineSvg(desc: XdmNode, filename: String) {
        toSvg(desc, filename, "/com/xmlcalabash/pipeline2dot.xsl")
    }

    private fun graphSvg(desc: XdmNode, filename: String) {
        toSvg(desc, filename, "/com/xmlcalabash/graph2dot.xsl")
    }

    private fun toSvg(desc: XdmNode, filename: String, stylesheet: String) {
        var styleStream = TestCase::class.java.getResourceAsStream(stylesheet)
        var styleSource = SAXSource(InputSource(styleStream))
        var xsltCompiler = desc.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = desc.processor.isSchemaAware
        var xsltExec = xsltCompiler.compile(styleSource)

        var transformer = xsltExec.load30()
        val xmlResult = XdmDestination()
        transformer.applyTemplates(desc.asSource(), xmlResult)
        val dotxml = xmlResult.xdmNode

        styleStream = TestCase::class.java.getResourceAsStream("/com/xmlcalabash/dot2txt.xsl")
        styleSource = SAXSource(InputSource(styleStream))
        xsltCompiler = desc.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = desc.processor.isSchemaAware
        xsltExec = xsltCompiler.compile(styleSource)

        transformer = xsltExec.load30()
        val textResult = RawDestination()
        transformer.applyTemplates(dotxml.asSource(), textResult)

        val tempFile = File.createTempFile("xmlcalabash-", ".dot")
        tempFile.deleteOnExit()

        val dot = PrintStream(tempFile)
        val iter = textResult.xdmValue.iterator()
        while (iter.hasNext()) {
            dot.print(iter.next().stringValue)
        }
        dot.close()

        val rt = Runtime.getRuntime()
        val args = arrayOf(graphviz!!.absoluteFile.toString(), "-Tsvg", tempFile.getAbsolutePath().toString(), "-o", filename)
        val process = rt.exec(args)
        process.waitFor()
        tempFile.delete()
    }

    override fun toString(): String {
        return testFile.toString()
    }

    private fun setupFileEnvironment(fileEnvironment: FileEnvironment) {
        val dir = File(testFile.parent).parent
        val folder = File(dir, "testfolder")
        recursiveRemove(folder)

        folder.mkdirs()
        for (prop in fileEnvironment.properties) {
            setupFileProperties(folder, prop)
        }
    }

    private fun setupFileProperties(folder: File, prop: TestFileProperties) {
        when (prop) {
            is TestFolder -> setupFolder(folder, prop)
            is TestFile -> setupFile(folder, prop)
            else -> Unit
        }
    }

    private fun setupFolder(root: File, prop: TestFolder) {
        val folder = File(root, prop.path)
        folder.mkdirs()
        setFileProperties(folder, prop)
    }

    private fun setupFile(folder: File, prop: TestFile) {
        val content = prop.content ?: ""
        val file = File(folder, prop.path)
        val parent = File(file.parent)
        parent.mkdirs()
        val stream = PrintStream(FileOutputStream(file))
        stream.print(content)
        stream.close()
        setFileProperties(file, prop)
    }

    private fun setFileProperties(inputFile: File, prop: TestFileProperties) {
        // Dealing with hidden files is a bit of a hack.
        var file = inputFile
        if (prop.hidden == true) {
            val path = file.toPath()
            if (isWindows) {
                Files.setAttribute(path, "dos:hidden", true)
            } else {
                // This is what "hidden" means on Linux/Mac filesystems.
                if (!file.name.startsWith(".")) {
                    file = File(file.parent, ".${file.name}")
                    inputFile.renameTo(file)
                }
            }
        }

        if (prop.lastModified != null) {
            val ft = FileTime.from(prop.lastModified.toInstant())
            Files.setLastModifiedTime(file.toPath(), ft)
        }

        val posix = mutableSetOf<PosixFilePermission>()
        if (file.isDirectory) {
            posix.add(PosixFilePermission.OWNER_EXECUTE)
        }
        if (prop.readable != false) {
            posix.add(PosixFilePermission.OWNER_READ)
        }
        if (prop.writable != false) {
            posix.add(PosixFilePermission.OWNER_WRITE)
        }

        Files.setPosixFilePermissions(file.toPath(), posix)
    }

    private fun teardownFileEnvironment() {
        val dir = File(testFile.parent).parent
        val folder = File(dir, "testfolder")
        recursiveRemove(folder)
        folder.mkdirs()
    }

    private fun recursiveRemove(dir: File) {
        if (!dir.exists()) {
            return
        }

        val perms = PosixFilePermissions.fromString("rwxr--r--")
        Files.setPosixFilePermissions(dir.toPath(), perms)

        if (dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    recursiveRemove(file)
                }
            } else {
                println("WAT? Null? " + dir.absoluteFile)
            }
        }

        dir.delete()
    }

    inner class FileEnvironment(val properties: List<TestFileProperties>)

    abstract inner class TestFileProperties(val path: String, val lastModified: Date? = null,
        val readable: Boolean? = null, val writable: Boolean? = null, val hidden: Boolean? = null) {
    }

    inner class TestFolder(path: String, lastModified: Date? = null,
                           readable: Boolean? = null, writable: Boolean? = null, hidden: Boolean?): TestFileProperties(path, lastModified, readable, writable, hidden)

    inner class TestFile(path: String, lastModified: Date? = null,
                         readable: Boolean? = null, writable: Boolean? = null, hidden: Boolean?,
                         val content: String? = null): TestFileProperties(path, lastModified, readable, writable, hidden)
}