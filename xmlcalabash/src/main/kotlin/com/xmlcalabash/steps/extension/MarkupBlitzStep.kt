package com.xmlcalabash.steps.extension

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.InvisibleXmlMarkupBlitz
import net.sf.saxon.s9api.XdmNode

class MarkupBlitzStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val sourceDoc = queues["source"]!!.first()
        val grammarDoc = queues["grammar"]!!.firstOrNull()
        val failOnError = booleanBinding(Ns.failOnError) != false
        val parameters = qnameMapBinding(Ns.parameters)

        val impl = InvisibleXmlMarkupBlitz(stepConfig)

        val source = (sourceDoc.value as XdmNode).underlyingValue.stringValue
        val grammar = (grammarDoc?.value as XdmNode?)?.underlyingValue?.stringValue

        val result = impl.parse(grammar, source, failOnError, parameters)
        receiver.output("result", result)
    }

    override fun toString(): String = "cx:markup-blitz"
}