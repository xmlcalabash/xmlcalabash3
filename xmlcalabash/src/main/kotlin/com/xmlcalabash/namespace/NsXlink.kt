package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsXlink {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/1999/xlink")

    val href = QName(namespace, "href")
}