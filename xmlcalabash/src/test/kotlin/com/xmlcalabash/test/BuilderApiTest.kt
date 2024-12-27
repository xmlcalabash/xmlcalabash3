package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.*
import com.xmlcalabash.runtime.XProcPipeline
import com.xmlcalabash.util.BufferingReceiver
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URI

class BuilderApiTest {
    private fun result(receiver: BufferingReceiver): String {
        val result = receiver.outputs["result"]?.removeFirstOrNull()
        Assertions.assertNotNull(result)
        val value = result!!.value as XdmNode
        val baos = ByteArrayOutputStream()
        val serializer = value.processor.newSerializer(baos)
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        serializer.serialize(value.underlyingNode)
        serializer.close()
        return String(baos.toByteArray())
    }

    @Test
    fun simplePipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        declStep.input("source")
        declStep.output("result")
        val identity = declStep.atomicStep(NsP.identity)
        identity.name = "test"

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc/>")

        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun defaultInputPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1

        val defaultInput = declStep.fromString("<default-input/>")

        val input = declStep.input("source")
        input.inline(defaultInput)
        declStep.output("result")
        val identity = declStep.atomicStep(NsP.identity)
        identity.name = "test"

        val pipeline = declStep.getExecutable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<default-input/>", xml)
    }

    private fun staticOptionPipelineSetup(sopt: String?): XProcPipeline {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()
        if (sopt != null) {
            builder.option(QName("sopt"), XdmAtomicValue("different"))
        }

        val sc = builder.stepConfig

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        declStep.output("result")
        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"

        val opt = declStep.option(QName("sopt"))
        opt.select = XProcExpression.constant(sc, XdmAtomicValue("test"))
        opt.static = true

        val wrapSequence = declStep.atomicStep(NsP.wrapSequence)
        wrapSequence.withOption(QName("wrapper"), XProcExpression.select(wrapSequence.stepConfig, "\$sopt"))

        val wi = wrapSequence.withInput()
        wi.inline(declStep.fromString("<test value='{\$sopt}'/>").value as XdmNode)

        val pipeline = declStep.getExecutable()
        return pipeline
    }

    @Test
    fun staticOptionPipeline() {
        val pipeline = staticOptionPipelineSetup(null)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertTrue(xml.contains("<test"))
        Assertions.assertTrue(xml.contains(" value=\"test") || xml.contains(" value='test"))
    }

    @Disabled
    fun staticOptionPipelineDifferent() {
        // I'm not convinced this use of the API should be expected to work
        val pipeline = staticOptionPipelineSetup("different")

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertTrue(xml.contains("<different"))
        Assertions.assertTrue(xml.contains(" value=\"different") || xml.contains(" value='different"))
    }

    @Test
    fun noVersionPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.input("source")
        declStep.output("result")
        declStep.atomicStep(NsP.identity, "test")

        try {
            declStep.getExecutable()
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(NsErr.xs(62), ex.error.code)
        }
    }

    @Test
    fun outOfOrderPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        val msource = declStep.input("source")
        declStep.output("result")

        val wrap = declStep.atomicStep(NsP.wrapSequence, "wrap")
        wrap.withOption(
            Ns.wrapper,
            XProcExpression.constant(wrap.stepConfig, XdmAtomicValue(QName(NamespaceUri.NULL, "wrapper")))
        )
        val winput = wrap.withInput("source")

        val identity = declStep.atomicStep(NsP.identity)
        val iinput = identity.withInput()
        iinput.pipe(msource)

        winput.pipe(identity)

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc/>")

        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun nestedPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test = QName(testns, "test:test")
        val sc = builder.stepConfig.with("test", testns)

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        declStep.input("source")
        declStep.output("result")

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val tstep = declStep.declareStep()
        tstep.name = "test"
        tstep.type = test_test
        tstep.input("source")
        tstep.output("result")
        tstep.atomicStep(NsP.identity)

        val testStep = declStep.atomicStep(test_test, "test")
        val wrap = declStep.atomicStep(NsP.wrapSequence, "wrap")
        val wrapInput = wrap.withInput("source")
        wrapInput.pipe("main", "source")
        wrapInput.pipe("test")

        val expr = XProcExpression.avt(sc, "{'aname-' || year-from-date(current-date()) idiv 1000}")
        wrap.withOption(Ns.wrapper, expr)

        val doc = testStep.fromString("<doc/>")

        val pipeline = declStep.getExecutable()
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<aname-2><doc/><doc/></aname-2>", xml)
    }

    @Test
    fun importedPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test = QName(testns, "test:test")
        val sc = builder.stepConfig.with("test", testns)

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val library = builder.newLibrary()
        library.version = 3.1

        val tstep = library.declareStep()
        tstep.name = "test"
        tstep.type = test_test
        tstep.input("source")
        tstep.output("result")
        tstep.atomicStep(NsP.identity)

        declStep.import(library)

        declStep.input("source")
        declStep.output("result")

        val testStep = declStep.atomicStep(test_test, "test")
        val wrap = declStep.atomicStep(NsP.wrapSequence, "wrap")
        val wrapInput = wrap.withInput("source")
        wrapInput.pipe("main", "source")
        wrapInput.pipe("test")

        val expr = XProcExpression.avt(sc, "{'aname-' || year-from-date(current-date()) idiv 1000}")
        wrap.withOption(Ns.wrapper, expr)

        val doc = testStep.fromString("<doc/>")

        val pipeline = declStep.getExecutable()
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<aname-2><doc/><doc/></aname-2>", xml)
    }

    @Test
    fun simpleLibrary() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test1 = QName(testns, "test:test1")
        val test_test2 = QName(testns, "test:test2")

        val library = builder.newLibrary()
        library.stepConfig.putNamespace("test", testns)
        library.version = 3.1

        val t1 = library.declareStep()
        t1.name = "test1"
        t1.type = test_test1
        t1.input("source")
        t1.output("result")
        t1.atomicStep(NsP.identity)

        val t2 = library.declareStep()
        t2.name = "test2"
        t2.type = test_test2
        t2.input("source")
        t2.output("result")
        t2.atomicStep(test_test1)

        val doc = library.fromString("<doc/>")

        val pipeline = library.getExecutable("test2")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun importDeclareStep() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test1 = QName(testns, "test:test1")

        val decl = builder.newDeclareStep()
        decl.version = 3.1

        val test = builder.newDeclareStep()
        test.version = 3.1
        test.type = test_test1
        test.input("source")
        test.output("result")
        test.atomicStep(NsP.identity)

        decl.import(test)

        decl.input("source")
        decl.output("result")

        decl.atomicStep(test_test1)

        val pipeline = decl.getExecutable()

        val doc = decl.fromString("<doc/>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun importLibrary() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test1 = QName(testns, "test:test1")

        val decl = builder.newDeclareStep()
        decl.version = 3.1

        //decl.debugPipelineAfter = "/tmp/pipeline.xml"
        //decl.debugPipelineGraph = "/tmp/graph.xml"

        val library = builder.newLibrary()

        val test = library.declareStep()
        test.type = test_test1
        test.input("source")
        test.output("result")
        test.atomicStep(NsP.identity)

        decl.import(test)

        decl.input("source")
        decl.output("result")

        decl.atomicStep(test_test1)

        val pipeline = decl.getExecutable()

        val doc = decl.fromString("<doc/>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun importRecursiveLibrary() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test1 = QName(testns, "test:test1")

        val decl = builder.newDeclareStep()
        decl.version = 3.1

        val library = builder.newLibrary()

        library.import(library)

        val test = library.declareStep()
        test.type = test_test1
        test.input("source")
        test.output("result")
        test.atomicStep(NsP.identity)

        decl.import(test)

        decl.input("source")
        decl.output("result")

        decl.atomicStep(test_test1)

        val pipeline = decl.getExecutable()

        val doc = decl.fromString("<doc/>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun importMutuallyRecursiveLibraries() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder(3.1)

        val testns = NamespaceUri.of("http://example.com/test")
        val test_test1 = QName(testns, "test:test1")
        val test_test2 = QName(testns, "test:test2")

        val decl = builder.newDeclareStep()

        val library1 = builder.newLibrary()
        library1.stepConfig.updateWith(URI("http://example.com/library1"))
        val library2 = builder.newLibrary()
        library2.stepConfig.updateWith(URI("http://example.com/library2"))

        library1.import(library2)
        library2.import(library1)

        val test1 = library1.declareStep()
        test1.type = test_test1
        test1.input("source")
        test1.output("result")
        test1.atomicStep(NsP.identity)

        val test2 = library2.declareStep()
        test2.type = test_test2
        test2.input("source")
        test2.output("result")
        test2.atomicStep(NsP.identity)

        decl.import(library1)

        decl.input("source")
        decl.output("result")

        decl.atomicStep(test_test1)
        decl.atomicStep(test_test2)

        val pipeline = decl.getExecutable()

        val doc = decl.fromString("<doc/>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun groupPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        declStep.input("source")
        declStep.output("result")

        val group = declStep.group()

        group.atomicStep(NsP.identity)

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc/>")

        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc/>", xml)
    }

    @Test
    fun forEachPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        input.sequence = true
        input.inline(input.fromString("<doc>One</doc>"))
        input.inline(input.fromString("<doc>Two</doc>"))

        val output = declStep.output("result")
        output.sequence = true

        val forEach = declStep.forEach()
        val addAttr = forEach.atomicStep(NsP.addAttribute)
        addAttr.withOption(Ns.match, "/*")
        addAttr.withOption(Ns.attributeName, "success")
        addAttr.withOption(Ns.attributeValue, "true")

        val pipeline = declStep.getExecutable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        var xml = result(receiver)
        Assertions.assertEquals("<doc success=\"true\">One</doc>", xml)
        xml = result(receiver)
        Assertions.assertEquals("<doc success=\"true\">Two</doc>", xml)
    }

    @Test
    fun choosePipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        input.inline(input.fromString("<doc>7</doc>"))

        declStep.output("result")

        val choose = declStep.choose()
        val when1 = choose.whenInstruction()
        when1.test = "string(.) = '4'"
        val id1 = when1.atomicStep(NsP.identity, "id1")
        val id1input = id1.withInput()
        id1input.inline(id1.fromString("<doc>Success when 4</doc>"))

        val when2 = choose.whenInstruction()
        when2.test = "string(.) = '7'"
        val id2 = when2.atomicStep(NsP.identity, "id2")
        val id2input = id2.withInput()
        id2input.inline(id2.fromString("<doc>Success when 7</doc>"))

        val otherwise = choose.otherwise()
        val id3 = otherwise.atomicStep(NsP.identity, "id3")
        val id3input = id3.withInput()
        id3input.inline(id3.fromString("<doc>Success when not 4 or 7 (was {string(.)})</doc>"))

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc>4</doc>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc>Success when 4</doc>", xml)
    }

    @Test
    fun ifTruePipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.stepConfig.putNamespace("xs", NsXs.namespace)
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        input.inline(input.fromString("<doc>6</doc>"))

        declStep.output("result")

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val ifStep = declStep.ifInstruction()
        ifStep.name = "if"
        ifStep.test = "xs:integer(.) mod 2 = 0"
        val ifid = ifStep.atomicStep(NsP.identity, "id-in-if")
        val ifinput = ifid.withInput()
        ifinput.inline(declStep.fromString("<doc>Success if input was even.</doc>"))

        val primin = declStep.atomicStep(NsP.identity, "primin")
        val priminput = primin.withInput()
        priminput.pipe = "@main"

        val copy = declStep.atomicStep(NsP.identity, "copy")
        val copyinput = copy.withInput()
        copyinput.inline(declStep.fromString("<doc>Testing with {string(.)}</doc>"))

        val wrapseq = declStep.atomicStep(NsP.wrapSequence, "wrapseq")
        wrapseq.withOption(Ns.wrapper, "wrapper")
        val wrapin = wrapseq.withInput()
        val pipe1 = wrapin.pipe()
        pipe1.step = "copy"
        val pipe2 = wrapin.pipe()
        pipe2.step = "if"

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc>4</doc>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals(
            "<wrapper><doc>Testing with 4</doc><doc>Success if input was even.</doc></wrapper>",
            xml
        )
    }

    @Test
    fun ifFalsePipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.stepConfig.putNamespace("xs", NsXs.namespace)
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        input.inline(input.fromString("<doc>7</doc>"))

        declStep.output("result")

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val ifStep = declStep.ifInstruction()
        ifStep.name = "if"
        ifStep.test = "xs:integer(.) mod 2 = 0"
        ifStep.output("r1", true)
        val out2 = ifStep.output("r2", false)
        val r2pipe = out2.pipe()
        r2pipe.step = "r2"

        val id_r2 = ifStep.atomicStep(NsP.identity, "r2")
        val id_r2_wi = id_r2.withInput()
        id_r2_wi.inline(declStep.fromString("<doc>This is a secondary result.</doc>"))

        val id = ifStep.atomicStep(NsP.identity)
        val id_wi = id.withInput()
        id_wi.inline(declStep.fromString("<doc>Success if the input was even.</doc>"))

        val wrapseq = declStep.atomicStep(NsP.wrapSequence, "wrapseq")
        wrapseq.withOption(Ns.wrapper, "both")
        val wrapin = wrapseq.withInput()
        val pipe1 = wrapin.pipe()
        pipe1.step = "if"
        val pipe2 = wrapin.pipe()
        pipe2.step = "if"
        pipe2.port = "r2"

        val pipeline = declStep.getExecutable()

        val doc = declStep.fromString("<doc>9</doc>")
        pipeline.input("source", doc)

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<both><doc>9</doc></both>", xml)
    }

    private fun tryCatchPipeline(even: Boolean) {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.stepConfig.putNamespace("xs", NsXs.namespace)
        declStep.stepConfig.putNamespace("cx", NsCx.errorNamespace)
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        if (even) {
            input.inline(input.fromString("<doc>6</doc>"))
        } else {
            input.inline(input.fromString("<doc>7</doc>"))
        }

        declStep.output("result")

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val tryCatch = declStep.tryInstruction()
        tryCatch.name = "try"

        val choose = tryCatch.choose()

        val cwhen = choose.whenInstruction()
        cwhen.test = "xs:integer(.) mod 2 = 0"
        val evenError = cwhen.atomicStep(NsP.error)
        evenError.withOption(Ns.code, "cx:even")

        val cother = choose.otherwise()
        val oddError = cother.atomicStep(NsP.error)
        oddError.withOption(Ns.code, "cx:odd")

        val oddCatch = tryCatch.catch()
        oddCatch.code = listOf(oddCatch.stepConfig.parseQName("cx:odd"))
        val id1 = oddCatch.atomicStep(NsP.identity)
        val wi1 = id1.withInput()
        wi1.inline(wi1.fromString("<doc>Successfully caught odd</doc>"))

        val evenCatch = tryCatch.catch()
        evenCatch.code = listOf(oddCatch.stepConfig.parseQName("cx:even"))
        val id2 = evenCatch.atomicStep(NsP.identity)
        val wi2 = id2.withInput()
        wi2.inline(wi2.fromString("<doc>Successfully caught even</doc>"))

        val finally = tryCatch.finally()
        val foutput = finally.output("finally", false)
        val pipe = foutput.pipe()
        pipe.step = "final"

        val id3 = finally.atomicStep(NsP.identity, "final")
        val wi3 = id3.withInput()
        wi3.inline(wi3.fromString("<doc>Finally</doc>"))

        val wrapseq = declStep.atomicStep(NsP.wrapSequence, "wrapseq")
        wrapseq.withOption(Ns.wrapper, "both")
        val wrapin = wrapseq.withInput()
        val pipe1 = wrapin.pipe()
        pipe1.step = "try"
        val pipe2 = wrapin.pipe()
        pipe2.step = "try"
        pipe2.port = "finally"

        val pipeline = declStep.getExecutable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        if (even) {
            Assertions.assertEquals("<both><doc>Successfully caught even</doc><doc>Finally</doc></both>", xml)
        } else {
            Assertions.assertEquals("<both><doc>Successfully caught odd</doc><doc>Finally</doc></both>", xml)
        }
    }

    @Test
    public fun tryCatchEven() {
        tryCatchPipeline(true)
    }

    @Test
    public fun tryCatchOdd() {
        tryCatchPipeline(false)
    }

    @Test
    fun viewportPipeline() {
        val calabash = XmlCalabash.newInstance()
        val builder = calabash.newPipelineBuilder()

        val declStep = builder.newDeclareStep()
        declStep.stepConfig.putNamespace("xs", NsXs.namespace)
        declStep.name = "main"
        declStep.version = 3.1
        val input = declStep.input("source")
        input.inline(input.fromString("<doc><ch>One</ch><ch>Two</ch></doc>"))

        declStep.output("result")

        //declStep.debugPipelineAfter = "/tmp/pipeline.xml"
        //declStep.debugPipelineGraph = "/tmp/graph.xml"

        val viewport = declStep.viewport()
        viewport.match = XProcExpression.match(viewport.stepConfig, "/doc/*")
        val addattr = viewport.atomicStep(NsP.addAttribute)
        addattr.withOption(Ns.attributeName, "success")
        addattr.withOption(Ns.attributeValue, XProcExpression.avt(addattr.stepConfig, "{p:iteration-position()}"))

        val pipeline = declStep.getExecutable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()

        val xml = result(receiver)
        Assertions.assertEquals("<doc><ch success=\"1\">One</ch><ch success=\"2\">Two</ch></doc>", xml)
    }
}