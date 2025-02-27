package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns

open class MessageStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val test = booleanBinding(Ns.test)!!
        if (test) {
            val message = stringBinding(Ns.select)!!
            stepConfig.info { message }
        }

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    override fun reset() {
        super.reset()
    }

    override fun toString(): String = "p:message"
}