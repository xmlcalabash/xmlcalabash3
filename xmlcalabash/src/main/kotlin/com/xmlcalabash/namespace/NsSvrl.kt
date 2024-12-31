package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSvrl {
    val namespace: NamespaceUri = NamespaceUri.of("http://purl.oclc.org/dsdl/svrl")

    val activePattern = QName(namespace, "svrl:active-pattern")
    val diagnosticReference = QName(namespace, "svrl:diagnostic-reference")
    val failedAssert = QName(namespace, "svrl:failed-assert")
    val firedRule = QName(namespace, "svrl:fired-rule")
    val schematronOutput = QName(namespace, "svrl:schematron-output")
    val successfulReport = QName(namespace, "svrl:successful-report")
    val text = QName(namespace, "svrl:text")
}