import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import net.sf.saxon.s9api.Processor
import org.junit.jupiter.api.Test

class SmokeTest {
    private val processor = Processor(false)

    @Test
    fun testEncryptedPDF() {
        val config = DefaultXmlCalabashConfiguration()
        val calabash = XmlCalabash.newInstance(config)
        //val xprocParser = PipelineParser(calabash.newPipelineBuilder())
        //val pipeline = xprocParser.parse("src/test/resources/pipe-encrypted-pdf.xpl")

        //val decl = pipeline as DeclareStepInstruction
        //val model = Graph.build(decl)

        //val runtime = XProcRuntime.build(model)
        //runtime.run()
    }
/*
        val receiver = BufferingOutputReceiver()
        try {
            exec.run(receiver)
        } catch (ex: Exception) {
            fail()
        }
        val result = receiver.read("result").first()
*/

/*
        val compiler = calabash.sa processor.newXPathCompiler()
        compiler.declareNamespace("xmp", "http://ns.adobe.com/xap/1.0/")
        val selector = compiler.compile("/ * /xmp:CreateDate").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("2018-10-19T00:22:35Z", node.underlyingValue.stringValue)

 */

/*
    private fun runPipeline(href: String): XProcDocument {
        val calabash = XmlCalabash.newInstance(processor)
        val runtime = calabash.compile(File("src/test/resources/pipe.xpl"))
        val exec = runtime.getExecutable()

        exec.option(QName("href"), href)

        val receiver = BufferingOutputReceiver()
        try {
            exec.run(receiver)
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.read("result").first()
        return result
    }

    @Test
    fun testPDF() {
        val result = runPipeline("envelope.pdf")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("dc", "http://purl.org/dc/elements/1.1/")
        val selector = compiler.compile("/ * /dc:title").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("An Envelope", node.underlyingValue.stringValue)
    }

    @Test
    fun testBMP() {
        val result = runPipeline("amaryllis.bmp")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("500", node.underlyingValue.stringValue)
    }

    @Test
    fun testEPS() {
        val result = runPipeline("amaryllis.eps")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Bounding Box']").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("-0 -0 500 336", node.underlyingValue.stringValue)
    }

    @Test
    fun testJPEG() {
        val result = runPipeline("amaryllis.jpg")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("500 pixels", node.underlyingValue.stringValue)
    }

    @Test
    fun testImagePDF() {
        val result = runPipeline("amaryllis.pdf")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /@width").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("500.0", node.underlyingValue.stringValue)
    }

    @Test
    fun testPNG() {
        val result = runPipeline("amaryllis.png")

        val compiler = processor.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode

        val node = selector.evaluate()

        Assertions.assertEquals("500", node.underlyingValue.stringValue)
    }

 */
}