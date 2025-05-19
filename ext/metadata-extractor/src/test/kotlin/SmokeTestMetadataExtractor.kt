import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.BufferingReceiver
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class SmokeTestMetadataExtractor {
    private fun runPipeline(pipeline: URI, href: String? = null): XProcDocument {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

        if (href != null) {
            exec.option(QName("href"), XProcDocument.ofText(href, decl.stepConfig))
        }

        val receiver = BufferingReceiver()
        exec.receiver = receiver
        try {
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        return result
    }

    @Test
    fun testEncryptedPDF() {
        val result = runPipeline(File("src/test/resources/pipe-encrypted-pdf.xpl").toURI())
        Assertions.assertTrue(result.value.toString().contains("xmp:ModifyDate"))
    }

    @Test
    fun testPDF() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "envelope.pdf")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("dc", "http://purl.org/dc/elements/1.1/")
        val selector = compiler.compile("/ * /dc:title").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("An Envelope", node.underlyingValue.stringValue)
    }

    @Test
    fun testBMP() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "amaryllis.bmp")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("500", node.underlyingValue.stringValue)
    }

    @Test
    fun testEPS() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "amaryllis.eps")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Bounding Box']").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("-0 -0 500 336", node.underlyingValue.stringValue)
    }

    @Test
    fun testJPEG() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "amaryllis.jpg")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("500 pixels", node.underlyingValue.stringValue)
    }

    @Test
    fun testImagePDF() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "amaryllis.pdf")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /@width").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("500.0", node.underlyingValue.stringValue)
    }

    @Test
    fun testPNG() {
        val result = runPipeline(File("src/test/resources/pipe.xpl").toURI(), "amaryllis.png")
        val compiler = result.context.newXPathCompiler()
        compiler.declareNamespace("c", "http://www.w3.org/ns/xproc-step")
        val selector = compiler.compile("/ * /c:tag[@name='Image Width']").load()
        selector.contextItem = result.value as XdmNode
        val node = selector.evaluate()
        Assertions.assertEquals("500", node.underlyingValue.stringValue)
    }
}