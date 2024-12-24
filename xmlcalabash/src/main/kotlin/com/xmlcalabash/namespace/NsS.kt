package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsS {
    val namespace: NamespaceUri = NamespaceUri.of("http://purl.oclc.org/dsdl/schematron")

    val schema = QName(namespace, "s:schema")

}