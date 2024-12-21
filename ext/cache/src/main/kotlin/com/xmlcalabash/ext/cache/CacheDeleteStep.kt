package com.xmlcalabash.ext.cache

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

class CacheDeleteStep(): AbstractAtomicStep() {
    companion object {
        val failIfNotInCache = QName(NamespaceUri.NULL, "fail-if-not-in-cache")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val failIfNotCached = booleanBinding(failIfNotInCache)!!
        val href = uriBinding(Ns.href)

        if (href == null && document.baseURI == null) {
            throw stepConfig.exception(XProcError.xiBaseUriRequiredToCache())
        }

        if (href != null) {
            if (failIfNotCached && stepConfig.environment.documentManager.getCached(href) == null) {
                throw stepConfig.exception(XProcError.xiDocumentNotInCache(href))
            }
            val props = DocumentProperties(document.properties)
            props.set(Ns.baseUri, href)
            stepConfig.environment.documentManager.uncache(document.with(props))
        } else {
            if (failIfNotCached && stepConfig.environment.documentManager.getCached(document.baseURI!!) == null) {
                throw stepConfig.exception(XProcError.xiDocumentNotInCache(document.baseURI!!))
            }
            stepConfig.environment.documentManager.uncache(document)
        }

        receiver.output("result", document)
    }

    override fun toString(): String = "cx:cache-delete"
}