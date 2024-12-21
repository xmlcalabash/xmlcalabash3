package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.parameters.StepParameters
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource

open class TextCountStep(): AbstractTextStep() {
    override fun run() {
        super.run()
        val document = queues["source"]!!.first()

        val count = textLines(document).size

        val result = "<c:result xmlns:c='${NsC.namespace}'>${count}</c:result>"
        val inputStream = ByteArrayInputStream(result.toByteArray(Charsets.UTF_8))
        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val xdm = builder.build(SAXSource(InputSource(inputStream)))
        receiver.output("result", XProcDocument.ofXml(xdm, stepConfig))
    }

    override fun toString(): String = "p:text-count"
}