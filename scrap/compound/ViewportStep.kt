package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.RuntimeCompoundStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.XmlViewportComposer
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class ViewportStep(pipelineConfig: XProcRuntime, val match: String): RuntimeCompoundStep(pipelineConfig) {

    override fun runStep() {
        val sequence = inputDocuments["!source"]!!
        inputDocuments.remove("!source")

        val bindings = mutableMapOf<QName, XdmValue>()
        val composer = XmlViewportComposer(stepConfig, match, bindings)

        val exec = stepConfig.newExecutionContext()
        stepConfig.setExecutionContext(exec)

        while (sequence.isNotEmpty()) {
            val document = sequence.removeFirst()
            val itemList = composer.decompose(document)

            var position = 1L
            exec.iterationSize = itemList.size.toLong()

            for (item in itemList) {
                exec.iterationPosition = position
                if (position > 1) {
                    for (step in steps) {
                        step.reset()
                    }
                }

                if (S9Api.isTextDocument(item.node)) {
                    head.input("current", XProcDocument.ofText(item.node, stepConfig, MediaType.TEXT, document.properties))
                } else {
                    head.input("current", XProcDocument.ofXml(item.node, stepConfig, document.properties))
                }

                for ((port, documents) in inputDocuments) {
                    for (otherDocument in documents) {
                        head.input(port, otherDocument)
                    }
                }

                head.run()
                head.receiver.close()
                val remaining = runTheseStepsExhaustively(steps)
                if (remaining.isNotEmpty()) {
                    throw XProcError.xiNoRunnableSteps().exception()
                }

                val nodes = mutableListOf<XdmNode>()
                for (doc in foot.queue("!result")) {
                    when (doc.value) {
                        is XdmNode -> nodes.add(doc.value as XdmNode)
                        else -> throw XProcError.xdViewportResultNotXml().exception()
                    }
                }
                foot.clear("!result")

                item.replaceWith(nodes)

                position++
            }

            val composed = composer.recompose().value

            println(composed)
        }

        inputDocuments.clear()
        stepConfig.runtime.releaseExecutionContext()
    }
}