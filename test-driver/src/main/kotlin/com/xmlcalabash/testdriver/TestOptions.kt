package com.xmlcalabash.testdriver

import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

class TestOptions() {
    var debug = false
    var title: String? = null
    var requirePass = false
    var outputGraph: String? = null
    var testRegex = "."
    var stopOnFirstFailed = false
    var report: String? = null
    var testDirectoryList = mutableListOf<String>()
    var consoleOutput = false
    val options = mutableMapOf<QName, XdmValue>()
}