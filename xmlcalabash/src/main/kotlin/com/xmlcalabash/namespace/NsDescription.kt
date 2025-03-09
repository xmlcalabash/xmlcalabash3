package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsDescription {
    val namespace: NamespaceUri = NamespaceUri.of("http://xmlcalabash.com/ns/description")

    val description = QName(namespace, "g:description")
    val graph = QName(namespace, "g:graph")
    val declareStep = QName(namespace, "g:declare-step")
    val compound = QName(namespace, "g:compound")
    val pipeline = QName(namespace, "g:pipeline")
    val subpipeline = QName(namespace, "g:subpipeline")
    val atomic = QName(namespace, "g:atomic")
    val head = QName(namespace, "g:head")
    val foot = QName(namespace, "g:foot")
    val edge = QName(namespace, "g:edge")
    val from = QName(namespace, "g:from")
    val to = QName(namespace, "g:to")
    val input = QName(namespace, "g:input")
    val output = QName(namespace, "g:output")
    val sink = QName(namespace, "g:sink")
}