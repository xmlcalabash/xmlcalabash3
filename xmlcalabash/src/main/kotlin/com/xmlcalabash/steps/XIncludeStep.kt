package com.xmlcalabash.steps

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

        val xincluder = XInclude()
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
}