package com.xmlcalabash.ext.rdf

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSparqlResults {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/2005/sparql-results#")

    val sparql = QName(namespace, "sparql")
    val head = QName(namespace, "head")
    val variable = QName(namespace, "variable")
    val link = QName(namespace, "link")
    val results = QName(namespace, "results")
    val result = QName(namespace, "result")
    val binding = QName(namespace, "binding")
    val bnode = QName(namespace, "bnode")
    val uri = QName(namespace, "uri")
    val literal = QName(namespace, "literal")
    val boolean = QName(namespace, "boolean")
}