import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun testTemplate() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse("src/test/resources/pipe.xpl")

        val exec = decl.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        System.out.println(result.value)
    }
}