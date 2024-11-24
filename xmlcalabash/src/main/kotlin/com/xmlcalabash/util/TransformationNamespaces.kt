package com.xmlcalabash.util

import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.namespace.NsXslt
import net.sf.saxon.om.NamespaceUri

class TransformationNamespaces(inScopeNamespaces: Map<String, NamespaceUri>) {
    private var _xslPrefix: String? = null
    private var _xsPrefix: String? = null
    val xslPrefix: String
        get() = _xslPrefix ?: "xsl"
    val xsPrefix: String
        get() = _xsPrefix ?: "xs"
    val namespaces = mutableMapOf<String,String>()

    init {
        for ((prefix,ns) in inScopeNamespaces) {
            namespaces[prefix] = ns.toString()
            if (ns == NsXslt.namespace) {
                _xslPrefix = prefix
            }
            if (ns == NsXs.namespace) {
                _xsPrefix = prefix
            }
        }

        if (_xslPrefix == null) {
            if (namespaces.containsKey("xsl")) {
                _xslPrefix = S9Api.uniquePrefix(namespaces.keys)
            } else {
                _xslPrefix = "xsl"
            }
        }
        namespaces[xslPrefix] = NsXslt.namespace.toString()

        if (_xsPrefix == null) {
            if (namespaces.containsKey("xs")) {
                _xsPrefix = S9Api.uniquePrefix(namespaces.keys)
            } else {
                _xsPrefix = "xs"
            }
        }
        namespaces[xsPrefix] = NsXs.namespace.toString()
    }
}