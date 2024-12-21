package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ExpressionEvaluator
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmItem

class FilterStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()

        val evaluator = ExpressionEvaluator(stepConfig.processor, stringBinding(Ns.select)!!)
        evaluator.setNamespaces(stepConfig.inscopeNamespaces)
        // FIXME: should only evaluate the bindings that are actually referenced...
        evaluator.setContext(document.value as XdmItem)
        val value = evaluator.evaluate()
        for (doc in S9Api.makeDocuments(stepConfig, value)) {
            receiver.output("result", doc)
        }
    }

    override fun toString(): String = "p:filter"
}