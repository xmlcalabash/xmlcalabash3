package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.steps.AbstractAtomicStep

open class UncacheDocument(): AbstractAtomicStep() {
    val cache = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        cache.add(doc)
    }

    override fun run() {
        super.run()
        while (cache.isNotEmpty()) {
            val doc = cache.removeFirst()
            if (doc.baseURI != null) {
                stepConfig.documentManager.uncache(doc)
            }
            receiver.output("result", doc);
        }
    }

    override fun toString(): String = "cx:cache-remove-document"
}