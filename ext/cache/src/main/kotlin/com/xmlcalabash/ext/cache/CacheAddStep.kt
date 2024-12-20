package com.xmlcalabash.ext.cache

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

class CacheAddStep(): AbstractAtomicStep() {
    companion object {
        val failIfInCache = QName(NamespaceUri.NULL, "fail-if-in-cache")
    }
    private lateinit var document: XProcDocument

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val failIfCached = booleanBinding(failIfInCache)!!
        val href = uriBinding(Ns.href)

        if (href == null && document.baseURI == null) {
            throw stepConfig.exception(XProcError.xiBaseUriRequiredToCache())
        }

        if (href != null) {
            if (failIfCached && stepConfig.environment.documentManager.getCached(href) != null) {
                throw stepConfig.exception(XProcError.xiDocumentInCache(href))
            }
            val props = DocumentProperties(document.properties)
            props.set(Ns.baseUri, href)
            stepConfig.environment.documentManager.cache(document.with(props))
        } else {
            if (failIfCached && stepConfig.environment.documentManager.getCached(document.baseURI!!) != null) {
                throw stepConfig.exception(XProcError.xiDocumentInCache(document.baseURI!!))
            }
            stepConfig.environment.documentManager.cache(document)
        }

        receiver.output("result", document)
    }

    override fun toString(): String = "cx:cache-add"
}