package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsHtml {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/1999/xhtml")

    val img = QName(namespace, "img")
    val body = QName(namespace, "body")
}
