package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsXvrl {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.xproc.org/ns/xvrl")

    val detection = QName(namespace, "xvrl:detection")
    val report = QName(namespace, "xvrl:report")
    val metadata = QName(namespace, "xvrl:metadata")
    val document = QName(namespace, "xvrl:document")
    val timestamp = QName(namespace, "xvrl:timestamp")
    val creator = QName(namespace, "xvrl:creator")
    val digest = QName(namespace, "xvrl:digest")
    val summary = QName(namespace, "xvrl:summary")
    val location = QName(namespace, "xvrl:location")
}