package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue
import java.net.URLDecoder

open class WwwFormUrlDecodeStep(): AbstractAtomicStep() {
    override fun input(port: String, doc: XProcDocument) {
        // nop, no inputs
    }

    override fun run() {
        super.run()

        val value = stringBinding(Ns.value)!!
        val encoding = stringBinding(NsCx.encoding) ?: "utf-8"
        val decoded = mutableMapOf<String, XdmValue>()

        // Do this the hard way because .split() doesn't do the right thing
        var pos: Int
        var part: String
        var parts = value
        while (parts != "") {
            pos = parts.indexOf("&")
            if (pos >= 0) {
                part = parts.substring(0, pos)
                parts = parts.substring(pos + 1).trim()
            } else {
                part = parts
                parts = ""
            }

            var keyname = part
            var keyvalue = ""
            pos = part.indexOf("=")
            if (pos >= 0) {
                keyname = URLDecoder.decode(part.substring(0, pos), encoding)
                keyvalue = URLDecoder.decode(part.substring(pos + 1), encoding)
            }

            if (part != "") {
                val kvalue = decoded[keyname] ?: XdmEmptySequence.getInstance()
                decoded[keyname] = kvalue.append(XdmAtomicValue(keyvalue))
            }
        }

        var json = XdmMap()
        for ((key, value) in decoded) {
            json = json.put(XdmAtomicValue(key), value)
        }

        receiver.output("result", XProcDocument.ofJson(json, stepConfig))
    }

    override fun toString(): String = "p:www-form-url-decode"
}