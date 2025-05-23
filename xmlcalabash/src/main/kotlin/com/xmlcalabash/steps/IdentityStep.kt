package com.xmlcalabash.steps

open class IdentityStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    override fun toString(): String = "p:identity"
}