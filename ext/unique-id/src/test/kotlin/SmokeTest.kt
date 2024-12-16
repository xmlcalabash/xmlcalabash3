import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.parsers.xpl.XplParser
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.collections.first

class SmokeTest {
    @Test
    fun testUniqueId() {
        val config = DefaultXmlCalabashConfiguration()
        val calabash = XmlCalabash.newInstance(config)
        val parser = calabash.newXProcParser()
        val decl = parser.parse(File("src/test/resources/pipe.xpl").toURI())
        val runtime = decl.runtime()
        val exec = runtime.executable()

        val receiver = BufferingReceiver()
        exec.receiver = receiver
        try {
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        println(result.value)
    }
}