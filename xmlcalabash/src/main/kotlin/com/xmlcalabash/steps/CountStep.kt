package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource
import kotlin.math.max
import kotlin.math.min

class CountStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val count = queues["source"]!!.size
        val limit = max(integerBinding(Ns.limit) ?: 0, 0)
        val reportedCount = if (limit == 0) {
            count
        } else {
            min(limit, count)
        }

        val result = "<c:result xmlns:c='${NsC.namespace}'>${reportedCount}</c:result>"
        val inputStream = ByteArrayInputStream(result.toByteArray(Charsets.UTF_8))
        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val xdm = builder.build(SAXSource(InputSource(inputStream)))
        receiver.output("result", XProcDocument.ofXml(xdm, stepConfig))
    }

    override fun toString(): String = "p:count"
}