package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.XmlViewportComposer
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.StringValue

open class ViewportStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    init {
        head.openPorts.remove("current") // doesn't count as an open port from the outside
    }

    override fun run() {
        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val outputPort = params.outputs.keys.first()

        val sequence = mutableListOf<XProcDocument>()
        sequence.addAll(cache["!source"] ?: emptyList())
        cache.remove("!source")

        var match = ""
        val bindings = mutableMapOf<QName, XdmValue>()

        if (head.options.containsKey(Ns.match)) {
            val matchMap = head.options[Ns.match]!!.first().value as XdmMap
            for (key in matchMap.keySet()) {
                val value = matchMap.get(key)
                val qkey = key.underlyingValue
                if (qkey is QNameValue) {
                    val lexical = if (qkey.prefix == null || qkey.prefix == "") {
                        qkey.localName
                    } else {
                        "${qkey.prefix}:${qkey.localName}"
                    }
                    bindings[QName(qkey.namespaceURI, lexical)] = value
                } else {
                    match = value.underlyingValue.stringValue
                }
            }
        } else {
            // It must have been resolved statically
            val matchMap = params.options[Ns.match]!!.staticValue!!.evaluate(stepConfig).underlyingValue as MapItem
            match = matchMap.get(StringValue("match")).stringValue
            if (matchMap.size() != 1) {
                throw stepConfig.exception(XProcError.xiImpossible("Unexpected values in static match expression"))
            }
        }

        val composer = XmlViewportComposer(stepConfig, match, bindings)

        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        stepConfig.saxonConfig.newExecutionContext(stepConfig)
        try {
            var firstTime = true

            while (sequence.isNotEmpty()) {
                val document = sequence.removeFirst()
                val itemList = composer.decompose(document)

                var position = 1L
                iterationSize = itemList.size.toLong()

                for (item in itemList) {
                    iterationPosition = position
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

                    val currentProperties = DocumentProperties(document.properties)
                    currentProperties[Ns.baseUri] = item.node.baseURI

                    head.cacheInputs(cache)
                    if (S9Api.isTextDocument(item.node)) {
                        head.input("current", XProcDocument.ofText(item.node, stepConfig, MediaType.TEXT, currentProperties))
                    } else {
                        head.input("current", XProcDocument.ofXml(item.node, stepConfig, currentProperties))
                    }

                    head.runStep(this)

                    runSubpipeline()

                    val nodes = mutableListOf<XdmNode>()
                    for (doc in foot.cache[outputPort] ?: emptyList()) {
                        // Different default here...
                        val ctc = doc.contentType?.classification() ?: MediaClassification.XML
                        if (ctc in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML, MediaClassification.TEXT)) {
                            when (doc.value) {
                                is XdmNode -> nodes.add(doc.value as XdmNode)
                                else -> throw stepConfig.exception(XProcError.xdViewportResultNotXml())
                            }
                        } else {
                            throw stepConfig.exception(XProcError.xdViewportResultNotXml())
                        }
                    }

                    item.replaceWith(nodes)

                    foot.cache.remove(outputPort)
                    position++
                }

                val composed = composer.recompose()

                foot.write("result", composed) // Always "result"
                foot.runStep(this)
            }

            cache.clear()
        } finally {
            stepConfig.saxonConfig.releaseExecutionContext()
        }
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("current")
    }
}