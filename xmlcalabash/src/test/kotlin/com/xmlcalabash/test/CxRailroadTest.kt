package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.BufferingReceiver
import net.sf.saxon.s9api.QName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class CxRailroadTest {
    private fun runPipelineGetAllResults(pipeline: URI, href: String? = null): List<XProcDocument> {
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

        return receiver.outputs["result"]!!.toList()
    }

    private fun runPipeline(pipeline: URI, href: String? = null): XProcDocument {
        val results = runPipelineGetAllResults(pipeline, href)
        return results.first()
    }

    @Test
    fun smokeTest001() {
        val result = runPipeline(File("src/test/resources/001-railroad.xpl").toURI())
        val xml = result.value.toString()
        Assertions.assertTrue(xml.startsWith("<svg"))
    }

}