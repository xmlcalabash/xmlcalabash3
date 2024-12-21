package com.xmlcalabash.steps.extension

import com.xmlcalabash.steps.AbstractAtomicStep

open class CacheDocument(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        for (doc in queues["source"]!!) {
            if (doc.baseURI != null) {
                stepConfig.environment.documentManager.cache(doc)
            }
            receiver.output("result", doc);
        }
    }

    override fun toString(): String = "cx:cache-add"
}