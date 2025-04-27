package com.xmlcalabash.steps

import com.nwalsh.sinclude.DefaultDocumentResolver
import com.nwalsh.sinclude.DocumentResolver
import com.nwalsh.sinclude.XInclude
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.XdmNode

open class XIncludeStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val defaultResolver = DefaultDocumentResolver()
        val xincluder = XInclude(XiResolver(defaultResolver))
        xincluder.trimText = booleanBinding(NsCx.trim) ?: false
        xincluder.fixupXmlBase = booleanBinding(Ns.fixupXmlBase) ?: false
        xincluder.fixupXmlLang = booleanBinding(Ns.fixupXmlLang) ?: false
        xincluder.copyAttributes = true // XInclude 1.1

        try {
            receiver.output("result", document.with(xincluder.expandXIncludes(document.value as XdmNode)))
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcXIncludeError(ex.message!!), ex)
        }
    }

    override fun toString(): String = "p:xinclude"

    inner class XiResolver(val defaultResolver: DocumentResolver): DocumentResolver {
        override fun resolveXml(base: XdmNode, uri: String, accept: String?, acceptLanguage: String?): XdmNode? {
            val href = UriUtils.resolve(base.baseURI, uri)!!
            val cached = stepConfig.environment.documentManager.getCached(href, MediaType.XML)
            if (cached == null) {
                return defaultResolver.resolveXml(base, uri, accept, acceptLanguage)
            }

            if (cached.value is XdmNode) {
                return cached.value as XdmNode
            }

            throw stepConfig.exception(XProcError.xcXIncludeError("Cached document is not XML: ${href}"))
        }

        override fun resolveText(base: XdmNode, uri: String, encoding: String?, accept: String?, acceptLanguage: String?): XdmNode? {
            val href = UriUtils.resolve(base.baseURI, uri)!!
            val cached = stepConfig.environment.documentManager.getCached(href, MediaType.TEXT)
            if (cached == null) {
                return defaultResolver.resolveText(base, uri, encoding, accept, acceptLanguage)
            }

            if (cached.value is XdmNode) {
                return cached.value as XdmNode
            }

            throw stepConfig.exception(XProcError.xcXIncludeError("Cached document is not a node: ${href}"))
        }
    }
}