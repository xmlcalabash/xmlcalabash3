import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.visualizers.Detail
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import kotlin.collections.first

class SmokeTestPipelineMessages {
    @Test
    fun runPipeline() {
        val pipeline = File("src/test/resources/pipe.xpl").toURI()
        val config = DefaultXmlCalabashConfiguration()
        config.visualizer = Detail(emptyMap())
        val calabash = XmlCalabash.newInstance(config)
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val runtime = decl.runtime()
        val exec = runtime.executable()

        val receiver = BufferingReceiver()
        try {
            exec.receiver = receiver
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        val node = S9Api.documentElement(result.value as XdmNode)
        Assertions.assertEquals(NsCx.messages, node.nodeName)
    }
}