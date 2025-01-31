package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSvg {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/2000/svg")

    val svg = QName(namespace, "svg")
    val def = QName(namespace, "def")
}