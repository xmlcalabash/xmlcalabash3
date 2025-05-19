import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.visualizers.Detail
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File

class SmokeTestPipelineMessages {
    @Test
    fun runPipeline() {
        val pipeline = File("src/test/resources/pipe.xpl").toURI()
        val xbuilder = XmlCalabashBuilder()
        xbuilder.setVisualizer("detail", emptyMap())
        val calabash = xbuilder.build()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

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