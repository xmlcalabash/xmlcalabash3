package com.xmlcalabash.testdriver

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.io.*
import java.net.InetAddress
import java.text.DecimalFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.system.exitProcess

class TestDriver(val testOptions: TestOptions, val exclusions: Map<String, String>) {
    lateinit var xmlCalabash: XmlCalabash
    val failedTests = mutableListOf<String>()
    val passedTests = mutableListOf<String>()
    val sortedTests = mutableListOf<TestCase>()
    var total = 0
    var pass = 0
    var fail = 0
    var notrun = 0
    var suiteElapsed: Double = -1.0

    fun run() {
        val buildDir = File("build")
        buildDir.mkdirs()

        val previousStatus = if (testOptions.prevRun != null) {
            File(buildDir, testOptions.prevRun!!)
        } else {
            File(buildDir, "test-suite-results.txt")
        }

        val statusDir = File(buildDir, "test-status")
        val statusFiles = statusDir.listFiles()

        if (statusFiles != null) {
            for (statusFile in statusFiles) {
                val lines = statusFile.readLines()
                val status = if (lines.isEmpty()) { "unknown" } else { lines.first() }
                if (status.startsWith("pass")) {
                    passedTests.add(statusFile.name)
                } else {
                    failedTests.add(statusFile.name)
                }
            }
        }

        val allTests = mutableListOf<File>()
        val skipTests = mutableMapOf<File, String>()
        val testPattern = "^.*${testOptions.testRegex}.*$".toRegex()
        for (testDirectory in testOptions.testDirectoryList) {
            val root = File(testDirectory)
            for (testFile in root.listFiles()!!) {
                if (testFile.name.endsWith(".xml")) {
                    val filename = testFile.name.substring(0, testFile.name.length - 4)
                    if (testPattern.matches(filename)) {
                        if (exclusions.contains(filename)) {
                            skipTests[testFile] = exclusions[filename]!!
                        } else {
                            allTests.add(testFile)
                        }
                    }
                }
            }
        }

        val sortedFiles = allTests.sortedWith(compareBy { it.toString() })
        allTests.clear()
        allTests.addAll(sortedFiles)

        val testsToRun = mutableListOf<File>()

        if (testOptions.firstFailed || testOptions.onlyFailedTests) {
            for (test in allTests) {
                if (failedTests.contains(test.name)) {
                    testsToRun.add(test)
                    if (testOptions.firstFailed) {
                        break
                    }
                }
            }
            if (testsToRun.isEmpty()) {
                println("Found no matching failed tests!")
            }
        }

        if (testOptions.onlyExpectedToPass) {
            for (test in allTests) {
                if (passedTests.contains(test.name)) {
                    testsToRun.add(test)
                }
            }
            if (testsToRun.isEmpty()) {
                println("Found no matching passed tests!")
            }
        }

        if (testsToRun.isEmpty()) {
            testsToRun.addAll(allTests)
        }

        xmlCalabash = XmlCalabash.newInstance()
        xmlCalabash.xmlCalabashConfig.uniqueInlineUris = false
        val saxonConfig = xmlCalabash.saxonConfig

        println("Running tests with ${saxonConfig.processor.saxonEdition} version ${saxonConfig.processor.saxonProductVersion}")
        val suite = TestSuite(xmlCalabash, testOptions)

        for (testFile in testsToRun) {
            suite.loadTest(testFile)
        }

        for ((testFile, _) in skipTests) {
            suite.loadTest(testFile)
        }

        sortedTests.clear()
        sortedTests.addAll(suite.testCases.keys.sortedWith(compareBy { it.testFile.toString() }))

        val repeat = 0
        val suiteStart = System.nanoTime()
        for (test in sortedTests) {
            test.singleTest = testOptions.stopOnFirstFailed || testsToRun.size == 1
            for (rep in 0..repeat) {
                if (test.testFile in skipTests) {
                    suite.skip(test, skipTests[test.testFile]!!)
                } else {
                    test.run()
                }
            }
            if (testOptions.stopOnFirstFailed) {
                var stop = false
                for (result in suite.testCases.values) {
                    stop = stop || result.status == "fail"
                }
                if (stop) {
                    break;
                }
            }
        }
        suiteElapsed = (System.nanoTime() - suiteStart) / 1e9

        for (test in sortedTests) {
            val status = suite.testCases[test]!!
            total++
            if (status.status == "pass") {
                pass++
            } else if (status.status == "fail") {
                fail++
            } else {
                notrun++
            }
        }

        val attempted = total + notrun
        val percent = "%.1f".format(100.0 * (1.0 * pass) / (1.0 * attempted))

        statusDir.mkdirs()

        makeReport()

        println("Test suite: ${pass} pass (${percent}%), ${fail} fail, ${notrun} not run (${total} total)")
        var max = 10
        for (testCase in sortedTests) {
            val status = suite.testCases[testCase]!!

            val testResult = File(statusDir, testCase.testFile.name)
            testResult.writeText("${status}\n")

            if (max > 0 && status.status == "fail") {
                max--
                if (status.error != null && status.expectedCodes.isNotEmpty()) {
                    println("FAIL: ${testCase.testFile} (${status.error.code} ≠ ${status.expectedCodes[0]})")
                } else if (status.failedAssertions.isNotEmpty()) {
                    println("FAIL: ${testCase.testFile}: ${status.failedAssertions[0].stringValue}${if (status.failedAssertions.size > 1) " ..." else ""}")
                } else {
                    println("FAIL: ${testCase.testFile}")
                }
            }
        }

        if (testsToRun.size > 1 && previousStatus.exists() && previousStatus.isFile) {
            val previous = mutableMapOf<String, String>()
            val reader = BufferedReader(InputStreamReader(FileInputStream(previousStatus)))
            var line = reader.readLine()
            while (line != null) {
                val pos = line.lastIndexOf(" ")
                if (pos > 0) {
                    previous[line.substring(pos + 1)] = line.substring(0, pos)
                }
                line = reader.readLine()
            }
            println("== Regressions ==========================================================")
            var newPass = 0
            var failingDifferently = 0
            for (testCase in sortedTests) {
                val status = suite.testCases[testCase]!!
                if (status.status != "NOTRUN") {
                    val prev = previous[testCase.testFile.toString()]
                    if (prev == null) {
                        println("NEW  ${status} ${testCase.testFile}")
                    } else if (prev == status.toString()) {
                        // no change
                    } else {
                        if (status.status == "pass") {
                            newPass++
                            //println("PASS ${testCase.testFile}")
                        } else {
                            if (prev.startsWith("fail")) {
                                failingDifferently++
                            } else {
                                println("FAIL ${prev}->${status} ${testCase.testFile}")
                            }
                        }
                    }
                }
            }
            if (newPass > 0) {
                println("${newPass} new passing tests")
            }
            if (failingDifferently > 0) {
                println("${failingDifferently} differently failing tests")
            }
        }

        if (testOptions.updateRegressions || !previousStatus.exists()) {
            val out = PrintStream(FileOutputStream(previousStatus))
            for (testCase in sortedTests) {
                val status = suite.testCases[testCase]!!
                out.println("${status} ${testCase.testFile}")
            }
        }

        if (testOptions.requirePass && fail > 0) {
            exitProcess(1)
        }
    }

    fun makeReport() {
        if (testOptions.report == null) {
            return
        }

        val saxonConfig = xmlCalabash.saxonConfig
        val report = SaxonTreeBuilder(saxonConfig.processor)
        report.startDocument(null)

        var stampStr = ZonedDateTime.now(ZoneId.of("UTC")).toString()
        stampStr = stampStr.substring(0, 19) + "Z" // Crudely remove fractional seconds

        val map = mutableMapOf<QName, String>()
        map[NsReport.name] = testOptions.title ?: "XProc Test Suite"
        map[NsReport.timestamp] = stampStr
        map[NsReport.time] = String.format("%1.4f", suiteElapsed)
        map[NsReport.hostname] = InetAddress.getLocalHost().hostName
        map[NsReport.tests] = total.toString()
        map[NsReport.errors] = fail.toString()
        map[NsReport.skipped] = notrun.toString()

        report.addStartElement(NsReport.testsuite, saxonConfig.valueConverter.attributeMap(map))

        report.addStartElement(NsReport.properties)
        val rte = saxonConfig.rteContext
        property(report, "processor", rte.productName)
        property(report, "version", rte.productVersion)
        property(report, "saxonVersion", "${saxonConfig.processor.saxonProductVersion}/${saxonConfig.processor.saxonEdition}")
        property(report, "vendor", rte.vendor)
        property(report, "vendorURI", rte.vendorUri)
        property(report, "xprocVersion", "3.1")
        property(report, "xpathVersion", rte.xpathVersion)
        property(report, "psviSupported", xmlCalabash.xmlCalabashConfig.schemaAware.toString())
        report.addEndElement()

        for (test in sortedTests) {
            map.clear()
            map[NsReport.name] = test.testFile.name
            map[NsReport.time] = String.format("%1.4f", test.elapsedSeconds)

            report.addStartElement(NsReport.testcase, saxonConfig.valueConverter.attributeMap(map))

            if (test.status.status == "skip") {
                report.addStartElement(NsReport.skipped)
                report.addText(test.status.message!!)
                report.addEndElement()
            } else {
                if (test.status.status == "fail") {
                    report.addStartElement(NsReport.failure)
                    report.addEndElement()
                }

                if (test.stdoutOutput.isNotEmpty()) {
                    report.addStartElement(NsReport.systemOut)
                    report.addText(test.stdoutOutput)
                    report.addEndElement()
                }

                if (test.stderrOutput.isNotEmpty()) {
                    report.addStartElement(NsReport.systemErr)
                    report.addText(test.stderrOutput)
                    report.addEndElement()
                }
            }


            report.addEndElement()
        }

        report.addEndElement()
        report.endDocument()

        val reportFile = File(testOptions.report!!)
        val stream = PrintStream(FileOutputStream(reportFile))
        stream.println(report.result)
        stream.close()
    }

    private fun property(report: SaxonTreeBuilder, key: String, value: String) {
        val saxonConfig = xmlCalabash.saxonConfig

        val map = mapOf( NsReport.name to key, NsReport.value to value )
        report.addStartElement(NsReport.property, saxonConfig.valueConverter.attributeMap(map))
        report.addEndElement()
    }

}