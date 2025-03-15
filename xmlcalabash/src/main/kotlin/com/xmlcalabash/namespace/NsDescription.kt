package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsDescription {
    val namespace: NamespaceUri = NamespaceUri.of("http://xmlcalabash.com/ns/description")

    // These are dynamic because the prefix isn't fixed.
    fun g(localName: String, prefix: String = "g"): QName {
        return QName(namespace, "${prefix}:${localName}")
    }
}