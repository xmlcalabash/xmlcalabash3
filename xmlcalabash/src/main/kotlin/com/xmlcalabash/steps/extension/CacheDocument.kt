package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.steps.AbstractAtomicStep

open class CacheDocument(): AbstractAtomicStep() {
    val cache = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        cache.add(doc)
    }

    override fun run() {
        super.run()
        while (cache.isNotEmpty()) {
            val doc = cache.removeFirst()
            if (doc.baseURI != null) {
                stepConfig.environment.documentManager.cache(doc)
            }
            receiver.output("result", doc);
        }
    }

    override fun toString(): String = "cx:cache-add-document"
}