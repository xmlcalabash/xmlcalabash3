package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File

class CxPipelineMessages {
    @Test
    fun runPipeline() {
        val pipeline = File("src/test/resources/001-pipeline-messages.xpl").toURI()
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