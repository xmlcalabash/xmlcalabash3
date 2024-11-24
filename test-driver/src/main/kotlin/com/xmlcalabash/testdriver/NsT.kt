package com.xmlcalabash.testdriver

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

class NsT {
    companion object {
        val namespace: NamespaceUri = NamespaceUri.of("http://xproc.org/ns/testsuite/3.0")
        val errorNamespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/ns/xproc-error")

        val test = QName(namespace, "t:test")
        val info = QName(namespace, "t:info")
        val description = QName(namespace, "t:description")
        val pipeline = QName(namespace, "t:pipeline")
        val input = QName(namespace, "t:input")
        val schematron = QName(namespace, "t:schematron")
        val option = QName(namespace, "t:option")
        val fileEnvironment = QName(namespace, "t:file-environment")
        val file = QName(namespace, "t:file")
        val folder = QName(namespace, "t:folder")
        val filetestContent = QName(namespace, "t:filetest-content")
    }
}