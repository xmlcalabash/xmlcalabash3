package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns

open class MessageStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val test = booleanBinding(Ns.test)!!
        if (test) {
            val value = options[Ns.select]!!.value
            for (item in value.iterator()) {
                stepConfig.info { item.toString() }
            }
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