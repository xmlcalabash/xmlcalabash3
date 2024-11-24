package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsFn {
    val namespace = NamespaceUri.of("http://www.w3.org/2005/xpath-functions")
    val errorNamespace = NamespaceUri.of("http://www.w3.org/2005/xqt-errors")
    val mapNamespace = NamespaceUri.of("http://www.w3.org/2005/xpath-functions/map")
    val arrayNamespace = NamespaceUri.of("http://www.w3.org/2005/xpath-functions/array")
    val mathNamespace = NamespaceUri.of("http://www.w3.org/2005/xpath-functions/math")

    val array = QName(namespace, "fn:array")
    val baseUri = QName(namespace, "fn:base-uri")
    val collection = QName(namespace, "fn:collection")
    val currentDate = QName(namespace, "fn:current-date")
    val currentDateTime = QName(namespace, "fn:current-dateTime")
    val currentTime = QName(namespace, "fn:current-time")
    val deepEqual = QName(namespace, "fn:deep-equal")
    val doc = QName(namespace, "fn:doc")
    val document = QName(namespace, "fn:document")
    val docAvailable = QName(namespace, "fn:doc-available")
    val map = QName(namespace, "fn:map")
    val unparsedText = QName(namespace, "fn:unparsed-text")
    val unparsedTextAvailable = QName(namespace, "fn:unparsed-text-available")

    val errXTSE3080 = QName(errorNamespace, "err:XTSE3080")
    val errXTMM9000 = QName(errorNamespace, "err:XTMM9000")
    val errXTDE0040 = QName(errorNamespace, "err:XTDE0040")
}