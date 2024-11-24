package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsC {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/ns/xproc-step")

    val archive = QName(namespace, "c:archive")
    val data = QName(namespace, "c:data")
    val directory = QName(namespace, "c:directory")
    val encoding = QName(namespace, "c:encoding")
    val environment = QName(namespace, "c:environment")
    val entry = QName(namespace, "c:entry")
    val errors = QName(namespace, "c:errors")
    val error = QName(namespace, "c:error")
    val file = QName(namespace, "c:file")
    val other = QName(namespace, "c:other")
    val param = QName(namespace, "c:param")
    val paramSet = QName(namespace, "c:param-set")
    val query = QName(namespace, "c:query")
    val report = QName(namespace, "c:report")
    val result = QName(namespace, "c:result")
}