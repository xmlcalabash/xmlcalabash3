package com.xmlcalabash.steps

import com.nwalsh.sinclude.DefaultDocumentResolver
import com.nwalsh.sinclude.DocumentResolver
import com.nwalsh.sinclude.XInclude
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.XdmNode

open class XIncludeStep(): AbstractAtomicStep() {
    lateinit var document: XProcDocument

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val defaultResolver = DefaultDocumentResolver()
        val xincluder = XInclude(XiResolver(defaultResolver))
        xincluder.trimText = booleanBinding(NsCx.trim) ?: false
        xincluder.fixupXmlBase = booleanBinding(Ns.fixupXmlBase) ?: false
        xincluder.fixupXmlLang = booleanBinding(Ns.fixupXmlLang) ?: false
        xincluder.copyAttributes = true // XInclude 1.1

        try {
            receiver.output("result", document.with(xincluder.expandXIncludes(document.value as XdmNode)))
        } catch (ex: Exception) {
            throw XProcError.xcXIncludeError(ex.message!!).exception(ex)
        }
    }

    override fun toString(): String = "p:xinclude"

    inner class XiResolver(val defaultResolver: DocumentResolver): DocumentResolver {
        override fun resolveXml(base: XdmNode, uri: String, accept: String?, acceptLanguage: String?): XdmNode? {
            val href = base.baseURI.resolve(uri)
            val cached = stepConfig.documentManager.getCached(href)
            if (cached == null) {
                return defaultResolver.resolveXml(base, uri, accept, acceptLanguage)
            }

            if (cached.value is XdmNode) {
                return cached.value as XdmNode
            }

            throw XProcError.xcXIncludeError("Cached document is not XML: ${href}").exception()
        }

        override fun resolveText(base: XdmNode, uri: String, encoding: String?, accept: String?, acceptLanguage: String?): XdmNode? {
            val href = base.baseURI.resolve(uri)
            val cached = stepConfig.documentManager.getCached(href)
            if (cached == null) {
                return defaultResolver.resolveText(base, uri, encoding, accept, acceptLanguage)
            }

            if (cached.value is XdmNode) {
                return cached.value as XdmNode
            }

            throw XProcError.xcXIncludeError("Cached document is not a node: ${href}").exception()
        }
    }
}