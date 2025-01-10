package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSaxon {
    val namespace = NamespaceUri.of("http://saxon.sf.net/")

    val hostLanguage = QName(namespace, "s:host-language")
    val static =QName(namespace, "s:static")
    val type = QName(namespace, "s:type")
    val expression = QName(namespace, "s:expression")
    val terminationMessage = QName(namespace, "s:termination-message")
    val alreadyReported = QName(namespace, "s:already-reported")
    val publicIdentifier = QName(namespace, "s:public-identifier")
    val constraintName = QName(namespace, "s:constraint-name")
    val constraintClause = QName(namespace, "s:constraint-clause")
    val constraintReference = QName(namespace, "s:constraint-reference")
    val schemaType = QName(namespace, "s:schema-type")
    val schemaPart = QName(namespace, "s:schema-part")
}