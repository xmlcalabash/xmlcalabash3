package com.xmlcalabash.testdriver

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.io.File

class TestSuite(val xmlCalabash: XmlCalabash, val options: TestOptions) {
    val testCases = mutableMapOf<TestCase, TestStatus>()
    private val _tests = mutableListOf<TestCase>()
    val tests: List<TestCase>
        get() {
            if (_tests.isEmpty()) {
                _tests.addAll(testCases.keys.sortedWith(compareBy { it.testFile.toString() }))
            }
            return _tests
        }

    fun loadTest(testFile: File): TestCase {
        println("Load ${testFile.absolutePath}")
        val case = TestCase(this, testFile)
        case.load()
        if (!options.onlyExpectedToPass || case.expected == "pass") {
            case.status = TestStatus("NOTRUN")
            testCases[case] = case.status
        }
        _tests.clear()
        return case
    }

    fun skipTest(testFile: File, reason: String) {
        val case = TestCase(this, testFile)
        case.load()
        case.status = TestStatus("SKIPPED")
        _tests.clear()
        testCases[case] = case.status
    }

    fun removeTest(testCase: TestCase) {
        testCases.remove(testCase)
        _tests.clear()
    }

    fun skip(case: TestCase, reason: String) {
        case.status = TestStatus("skip", reason)
        testCases[case] = case.status
    }

    fun pass(case: TestCase) {
        case.status = TestStatus("pass")
        testCases[case] = case.status
    }

    fun fail(case: TestCase) {
        case.status = TestStatus("fail")
        testCases[case] = case.status
    }

    fun fail(case: TestCase, error: XProcError) {
        case.status = TestStatus("fail", error)
        testCases[case] = case.status
    }

    fun fail(case: TestCase, failedAssertions: List<XdmNode>) {
        case.status = TestStatus("fail", failedAssertions)
        testCases[case] = case.status
    }

    fun fail(case: TestCase, error: XProcError, codes: List<QName>) {
        case.status = TestStatus("fail", error, codes)
        testCases[case] = case.status
    }
}