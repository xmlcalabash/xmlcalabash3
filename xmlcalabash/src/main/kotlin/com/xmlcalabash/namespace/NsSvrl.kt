package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSvrl {
    val namespace: NamespaceUri = NamespaceUri.of("http://purl.oclc.org/dsdl/svrl")

    val successfulReport = QName(namespace, "svrl:successful-report")
    val failedAssert = QName(namespace, "svrl:failed-assert")
    val text = QName(namespace, "svrl:text")
}