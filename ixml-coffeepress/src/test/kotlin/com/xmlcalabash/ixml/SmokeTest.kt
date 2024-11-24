package com.xmlcalabash.ixml

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun testCoffeePress() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val declaration = parser.parse("src/test/resources/pipe.xpl")

        val exec = declaration.getExecutable();
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