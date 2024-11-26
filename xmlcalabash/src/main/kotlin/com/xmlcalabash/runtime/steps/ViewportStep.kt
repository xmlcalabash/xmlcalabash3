package com.xmlcalabash.runtime.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.parameters.ViewportStepParameters
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.XmlViewportComposer
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

open class ViewportStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): CompoundStep(yconfig, compound) {
    val match = (params as ViewportStepParameters).match

    init {
        head.openPorts.remove("current") // doesn't count as an open port from the outside
    }

    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val outputPort = params.outputs.keys.first()

        val sequence = mutableListOf<XProcDocument>()
        sequence.addAll(cache["!source"] ?: emptyList())
        cache.remove("!source")

        val bindings = mutableMapOf<QName, XdmValue>()
        val composer = XmlViewportComposer(stepConfig, match, bindings)

        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(runnables)

        val exec = stepConfig.newExecutionContext(stepConfig)
        var firstTime = true

        while (sequence.isNotEmpty()) {
            val document = sequence.removeFirst()
            val itemList = composer.decompose(document)

            var position = 1L
            exec.iterationSize = itemList.size.toLong()

            for (item in itemList) {
                exec.iterationPosition = position
                if (!firstTime) {
                    head.reset()
                    head.showMessage = false
                    foot.reset()
                    for (step in stepsToRun) {
                        step.reset()
                    }
                } else {
                    firstTime = false
                }

                head.cacheInputs(cache)
                if (S9Api.isTextDocument(item.node)) {
                    head.input("current", XProcDocument.ofText(item.node, stepConfig, MediaType.TEXT, document.properties))
                } else {
                    head.input("current", XProcDocument.ofXml(item.node, stepConfig, document.properties))
                }

                head.runStep()

                val remaining = runStepsExhaustively(stepsToRun)
                if (remaining.isNotEmpty()) {
                    throw XProcError.xiNoRunnableSteps().exception()
                }

                val nodes = mutableListOf<XdmNode>()
                for (doc in foot.cache[outputPort] ?: emptyList()) {
                    val ct = doc.contentType ?: MediaType.XML
                    if (ct.xmlContentType() || ct.htmlContentType() || ct.textContentType()) {
                        when (doc.value) {
                            is XdmNode -> nodes.add(doc.value as XdmNode)
                            else -> throw XProcError.xdViewportResultNotXml().exception()
                        }
                    } else {
                        throw XProcError.xdViewportResultNotXml().exception()
                    }
                }

                item.replaceWith(nodes)

                foot.cache.remove(outputPort)
                position++
            }

            val composed = composer.recompose()

            foot.write("result", composed) // Always "result"
            foot.runStep()
        }

        cache.clear()
        stepConfig.releaseExecutionContext()
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("current")
    }
}