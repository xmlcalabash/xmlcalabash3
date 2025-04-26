package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue
import java.nio.charset.StandardCharsets

open class WwwFormUrlEncodeStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val parameters = parseParameters()
        val encoded = StringBuilder()
        var sep = ""

        for ((name, value) in parameters) {
            encoded.append(sep)
            sep = "&"

            if (value.size() == 0) {
                encoded.append(name)
                encoded.append("=")
            } else if (value.size() == 1) {
                encoded.append(name)
                encoded.append("=")
                encoded.append(UriUtils.encodeForUri(value.toString()))
            } else {
                var first = true
                val iter = value.iterator()
                while (iter.hasNext()) {
                    if (!first) {
                        encoded.append(sep)
                    }
                    first = false
                    encoded.append(name)
                    encoded.append("=")
                    encoded.append(UriUtils.encodeForUri(iter.next().toString()))
                }
            }
        }

        receiver.output("result", XProcDocument.ofText(encoded.toString(), stepConfig))
    }

    private fun parseParameters(): Map<String,XdmValue> {
        val value = options[Ns.parameters]!!.value
        if (value == XdmEmptySequence.getInstance()) {
            return mapOf()
        }
        val map = mutableMapOf<String,XdmValue>()
        for (key in (value as XdmMap).keySet()) {
            map.put(key.stringValue, value.get(key))
        }
        return map
    }

    override fun toString(): String = "p:www-form-url-decode"
}