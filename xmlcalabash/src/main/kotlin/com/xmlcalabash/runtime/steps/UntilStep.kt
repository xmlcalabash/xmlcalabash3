package com.xmlcalabash.runtime.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.XmlViewportComposer
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.StringValue

open class UntilStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    companion object {
        val cx_previous = QName(NsCx.namespace, "cx:previous")
    }

    init {
        head.openPorts.remove("current") // doesn't count as an open port from the outside
        foot.holdPorts.add("result")
    }

    override fun run() {
        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        val outputPort = params.outputs.keys.first()

        val sequence = mutableListOf<XProcDocument>()
        sequence.addAll(cache["!source"] ?: emptyList())
        cache.remove("!source")

        if (sequence.size != 1) {
            stepConfig.exception(XProcError.xdSequenceForbidden())
        }

        var document = sequence.removeFirst()
        var nextDocument = document

        val testExpression = if (head.options.containsKey(Ns.test)) {
            head.options[Ns.test]!!.first().value.underlyingValue.stringValue
        } else {
            val opt = head.staticOptions[Ns.test]!!
            opt.staticValue.evaluate(stepConfig).underlyingValue.stringValue
        }

        val compiler = stepConfig.newXPathCompiler()
        compiler.declareVariable(cx_previous)
        val xpathExec = compiler.compile(testExpression)
        val selector = xpathExec.load()
        var done = false

        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        stepConfig.saxonConfig.newExecutionContext(stepConfig)
        try {
            var position = 0L

            while (!done) {
                position++
                iterationSize = position
                iterationPosition = position

                if (position > 1L) {
                    head.reset()
                    head.showMessage = false
                    foot.reset()
                    for (step in stepsToRun) {
                        step.reset()
                    }
                }

                head.cacheInputs(cache)
                head.input("current", document)

                head.runStep(this)

                runSubpipeline()

                val result = foot.cache.remove(outputPort)
                if (result == null || result.size != 1) {
                    throw stepConfig.exception(XProcError.xdSequenceForbidden())
                }

                val nextDocument = result.first()
                selector.setVariable(cx_previous, document.value)
                if (nextDocument.value is XdmItem) {
                    selector.contextItem = nextDocument.value as XdmItem
                }
                done = selector.effectiveBooleanValue()

                document = nextDocument
            }

            foot.write("result", document) // Always "result"
            foot.runStep(this)

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