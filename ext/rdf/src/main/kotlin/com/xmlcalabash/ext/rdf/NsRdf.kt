package com.xmlcalabash.ext.rdf

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsRdf {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/1999/02/22-rdf-syntax-ns#")

    val RDF = QName(namespace, "rdf:RDF")

}