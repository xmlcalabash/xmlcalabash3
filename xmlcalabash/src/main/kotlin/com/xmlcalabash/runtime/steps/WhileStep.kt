package com.xmlcalabash.runtime.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
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

open class WhileStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
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

        val testExpression = if (head.options.containsKey(Ns.test)) {
            head.options[Ns.test]!!.first().value.underlyingValue.stringValue
        } else {
            val opt = head.staticOptions[Ns.test]!!
            opt.staticValue.evaluate(stepConfig).underlyingValue.stringValue
        }

        val compiler = stepConfig.newXPathCompiler()
        val xpathExec = compiler.compile(testExpression)
        val selector = xpathExec.load()
        if (document.value is XdmItem) {
            selector.contextItem = document.value as XdmItem
        }
        var runSubpipeline = selector.effectiveBooleanValue()

        stepsToRun.clear()
        stepsToRun.addAll(runnables)

        val exec = stepConfig.saxonConfig.newExecutionContext(stepConfig)
        var position = 0L

        while (runSubpipeline) {
            position++
            exec.iterationSize = position
            exec.iterationPosition = position

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

            head.runStep()

            runSubpipeline()

            val result = foot.cache.remove(outputPort)
            if (result == null || result.size != 1) {
                throw stepConfig.exception(XProcError.xdSequenceForbidden())
            }

            document = result.first()
            if (document.value is XdmItem) {
                selector.contextItem = document.value as XdmItem
            }
            runSubpipeline = selector.effectiveBooleanValue()
        }

        foot.write("result", document) // Always "result"
        foot.runStep()

        cache.clear()
        stepConfig.saxonConfig.releaseExecutionContext()
    }

    override fun reset() {
        super.reset()
        head.openPorts.remove("current")
    }
}