package com.xmlcalabash.testdriver

class TestOptions() {
    var title: String? = null
    var requirePass = false
    var traceExecution = false
    var outputDescription: String? = null
    var outputGraph: String? = null
    var testLogging = false
    var updateRegressions = false
    var testRegex = "."
    var firstFailed = false
    var onlyFailedTests = false
    var onlyExpectedToPass = false
    var stopOnFirstFailed = false
    var report: String? = null
    var testDirectoryList = mutableListOf<String>()
    var prevRun: String? = null
}