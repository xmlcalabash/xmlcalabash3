package com.xmlcalabash.datamodel

import org.apache.hc.core5.http.NameValuePair

class MediaTypeParameter(val parameter: String): NameValuePair {
    constructor (name: String, value: String) : this("${name}=${value}")

    val pos = parameter.indexOf("=")

    override fun getName(): String {
        if (pos > 0) {
            return parameter.substring(0, pos)
        }
        return ""
    }

    override fun getValue(): String {
        if (pos >= 0) {
            return parameter.substring(pos + 1)
        }
        return parameter
    }

    override fun toString(): String {
        return parameter
    }
}