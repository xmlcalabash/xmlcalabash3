package com.xmlcalabash.steps.extension

import com.xmlcalabash.steps.AbstractAtomicStep

open class UncacheDocument(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        for (doc in queues["source"]!!) {
            if (doc.baseURI != null) {
                stepConfig.environment.documentManager.uncache(doc)
            }
            receiver.output("result", doc);
        }
    }

    override fun toString(): String = "cx:cache-delete"
}