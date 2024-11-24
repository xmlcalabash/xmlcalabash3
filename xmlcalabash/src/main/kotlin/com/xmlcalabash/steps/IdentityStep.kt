package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument

open class IdentityStep(): AbstractAtomicStep() {
    val cache = mutableListOf<XProcDocument>()

    // In principle, input() can just immediately send the output to the receiver.
    // But in practice, it's easier to understand the runtime behavior of the
    // pipeline if we cache and then send them when the step actually runs.
    override fun input(port: String, doc: XProcDocument) {
        cache.add(doc)
    }

    override fun run() {
        super.run()
        while (cache.isNotEmpty()) {
            receiver.output("result", cache.removeFirst());
        }
    }

    override fun reset() {
        super.reset()
        cache.clear()
    }

    override fun toString(): String = "p:identity"
}