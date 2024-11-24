package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsXsi {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/2001/XMLSchema-instance")

    val noNamespaceSchemaLocation = QName(namespace, "xsi:noNamespaceSchemaLocation")
    val schemaLocation = QName(namespace, "xsi:schemaLocation")
    val type = QName(namespace, "xsi:type")

}