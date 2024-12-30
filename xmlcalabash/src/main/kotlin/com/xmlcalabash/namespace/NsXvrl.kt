package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsXvrl {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.xproc.org/ns/xvrl")

    val category = QName(namespace, "xvrl:category")
    val context = QName(namespace, "xvrl:context")
    val creator = QName(namespace, "xvrl:creator")
    val detection = QName(namespace, "xvrl:detection")
    val digest = QName(namespace, "xvrl:digest")
    val document = QName(namespace, "xvrl:document")
    val let = QName(namespace, "xvrl:let")
    val location = QName(namespace, "xvrl:location")
    val message = QName(namespace, "xvrl:message")
    val metadata = QName(namespace, "xvrl:metadata")
    val provenance = QName(namespace, "xvrl:provenance")
    val report = QName(namespace, "xvrl:report")
    val reports = QName(namespace, "xvrl:reports")
    val schema = QName(namespace, "xvrl:schema")
    val summary = QName(namespace, "xvrl:summary")
    val supplemental  = QName(namespace, "xvrl:supplemental")
    val timestamp = QName(namespace, "xvrl:timestamp")
    val title = QName(namespace, "xvrl:title")
    val validator = QName(namespace, "xvrl:validator")
    val valueOf = QName(namespace, "xvrl:value-of")
}