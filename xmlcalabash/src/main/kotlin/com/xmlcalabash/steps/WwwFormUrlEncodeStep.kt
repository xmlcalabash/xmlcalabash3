package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
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
                encoded.append(encode(value.toString()))
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
                    encoded.append(encode(iter.next().toString()))
                }
            }
        }

        receiver.output("result", XProcDocument.ofText(encoded.toString(), stepConfig))
    }

    private fun encode(value: String): String {
        val genDelims = ":/?#[]@"
        val subDelims = "!$'()*,;=" // N.B. no "&" and no "+" !
        val unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
        val okChars = genDelims + subDelims + unreserved

        val encoded = StringBuilder()
        for (byte in value.toByteArray(StandardCharsets.UTF_8)) {
            // Whoever decided that bytes should be signed needs their head examined
            val bint = if (byte.toInt() < 0) {
                byte.toInt() + 256
            } else {
                byte.toInt()
            }
            val ch = Char(bint)
            if (okChars.indexOf(ch) >= 0) {
                encoded.append(ch)
            } else {
                if (ch == ' ') {
                    encoded.append("+")
                } else {
                    encoded.append(String.format("%%%02X", ch.code))
                }
            }
        }

        return encoded.toString()
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