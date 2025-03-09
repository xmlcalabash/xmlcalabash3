package com.xmlcalabash.tracing

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsTrace {
    val namespace: NamespaceUri = NamespaceUri.of("https://xmlcalabash.com/ns/trace")

    val trace = QName(namespace, "trace")
    val thread = QName(namespace, "thread")
    val step = QName(namespace, "step")
    val document = QName(namespace, "document")
    val resource = QName(namespace, "resource")
    val from = QName(namespace, "from")
    val to = QName(namespace, "to")
    val location = QName(namespace, "location")
}