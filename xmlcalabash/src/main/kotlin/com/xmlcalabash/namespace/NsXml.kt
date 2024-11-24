package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsXml {
    val namespace = NamespaceUri.of("http://www.w3.org/XML/1998/namespace")

    val base = QName(namespace, "xml:base")
    val id = QName(namespace, "xml:id")
}