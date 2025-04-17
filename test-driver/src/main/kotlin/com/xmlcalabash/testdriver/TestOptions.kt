package com.xmlcalabash.testdriver

class TestOptions() {
    var title: String? = null
    var requirePass = false
    var outputGraph: String? = null
    var testRegex = "."
    var stopOnFirstFailed = false
    var report: String? = null
    var testDirectoryList = mutableListOf<String>()
    var consoleOutput = false
}