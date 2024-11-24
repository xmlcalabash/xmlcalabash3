import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun testUniqueId() {
        val calabash = XmlCalabash.newInstance(DefaultXmlCalabashConfiguration())
        //val xprocParser = PipelineParser(calabash.newPipelineBuilder())
        //val pipeline = xprocParser.parse("src/test/resources/pipe.xpl")

        //val decl = pipeline as DeclareStepInstruction
        //val model = Graph.build(decl)

        //val runtime = XProcRuntime.build(model)
        //runtime.run()
    }
}