package com.xmlcalabash.testdriver

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

class NsReport {
    companion object {
        val namespace: NamespaceUri = NamespaceUri.NULL

        val errors = QName("errors")
        val failure = QName("failure")
        val hostname = QName("hostname")
        val name = QName("name")
        val path = QName("path")
        val properties = QName("properties")
        val property = QName("property")
        val skipped = QName("skipped")
        val systemErr = QName("system-err")
        val systemOut = QName("system-out")
        val testcase = QName("testcase")
        val tests = QName("tests")
        val testsuite = QName("testsuite")
        val time = QName("time")
        val timestamp = QName("timestamp")
        val value = QName("value")
    }
}