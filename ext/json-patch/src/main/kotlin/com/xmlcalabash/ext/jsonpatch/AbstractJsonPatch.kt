package com.xmlcalabash.ext.jsonpatch

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

abstract class AbstractJsonPatch: AbstractAtomicStep() {
    protected fun jsonString(doc: XProcDocument): String {
        val params = mapOf<QName, XdmValue>(
            Ns.method to XdmAtomicValue("json"),
            Ns.indent to XdmAtomicValue(true)
        )
        val baos = ByteArrayOutputStream()
        val writer = DocumentWriter(doc, baos, params)
        writer.write()
        return baos.toString(StandardCharsets.UTF_8)
    }
}