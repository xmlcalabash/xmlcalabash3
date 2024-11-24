package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.parameters.SelectStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmItem

open class SelectStep(val params: SelectStepParameters): AbstractAtomicStep() {
    val documents = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        if (doc.value is XdmItem) {
            documents.add(doc)
        } else {
            throw RuntimeException("Attempt to select on something that isn't an xdmitem?")
        }
    }

    override fun run() {
        super.run()

        for (document in documents) {
            params.select.reset()
            params.select.contextItem = document

            for (vname in params.select.variableRefs) {
                if (options.containsKey(vname)) {
                    params.select.setBinding(vname, options[vname]!!.value)
                }
            }

            val result = params.select.evaluate()
            for (doc in S9Api.makeDocuments(stepConfig, result)) {
                receiver.output("result", doc)
            }
        }
    }

    override fun reset() {
        super.reset()
        documents.clear()
    }

    override fun toString(): String = "cx:selector"
}