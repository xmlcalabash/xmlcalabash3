package com.xmlcalabash.testdriver

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.util.BufferingMessageReporter
import com.xmlcalabash.util.NopMessageReporter
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.AssertionsLevel
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.s9api.QName
import net.sf.saxon.type.BuiltInAtomicType
import java.io.*
import java.net.InetAddress
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.system.exitProcess

class TestDriver(val testOptions: TestOptions, val exclusions: Map<String, String>) {
    val failedTests = mutableListOf<String>()
    val passedTests = mutableListOf<String>()
    val allTestCases = mutableListOf<TestCase>()
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

        // We need different XmlCalabash instances for the different configurations.
        // licensed/unlicensed
        // eager/notEager
        val builder = XmlCalabashBuilder()

        val eagerLicensed = builder
            .setLicensed(true)
            .setEagerEvaluation(true)
            .setUniqueInlineUris(false)
            .setAssertions(AssertionsLevel.WARNING)
            .setMessageReporter(BufferingMessageReporter(100, NopMessageReporter()))
            .setGraphviz(File("/opt/homebrew/bin/dot")) // FIXME:
            .build()

        val lazyLicensed = builder
            .setEagerEvaluation(false)
            .setMessageReporter(BufferingMessageReporter(100, NopMessageReporter()))
            .build()

        val eagerUnlicensed = builder
            .setLicensed(false)
            .setEagerEvaluation(true)
            .setMessageReporter(BufferingMessageReporter(100, NopMessageReporter()))
            .build()

        val lazyUnlicensed = XmlCalabashBuilder()
            .setLicensed(false)
            .setEagerEvaluation(false)
            .setMessageReporter(BufferingMessageReporter(100, NopMessageReporter()))
            .build()

        val saxonConfig = eagerLicensed.saxonConfiguration
        println("Running tests with ${saxonConfig.processor.saxonEdition} version ${saxonConfig.processor.saxonProductVersion}")

        val eagerLicensedSuite = TestSuite(eagerLicensed, testOptions)
        val eagerUnlicensedSuite = TestSuite(eagerUnlicensed, testOptions)
        val lazyLicensedSuite = TestSuite(lazyLicensed, testOptions)
        val lazyUnlicensedSuite = TestSuite(lazyUnlicensed, testOptions)

        for (testFile in testsToRun) {
            var case = lazyLicensedSuite.loadTest(testFile)
            if (case.features.contains("no-psvi-support")) {
                lazyLicensedSuite.removeTest(case)
                if (case.features.contains("eager-eval")) {
                    eagerUnlicensedSuite.loadTest(testFile)
                } else {
                    lazyUnlicensedSuite.loadTest(testFile)
                }
            } else {
                if (case.features.contains("eager-eval")) {
                    lazyLicensedSuite.removeTest(case)
                    eagerLicensedSuite.loadTest(testFile)
                }
            }
        }

        for ((testFile, _) in skipTests) {
            lazyLicensedSuite.loadTest(testFile)
        }

        val allSuites = mutableListOf<TestSuite>()
        for (suite in listOf(lazyLicensedSuite, lazyUnlicensedSuite, eagerLicensedSuite, eagerUnlicensedSuite)) {
            if (suite.testCases.isNotEmpty()) {
                allSuites.add(suite)
            }
        }

        val suiteStart = System.nanoTime()
        for (suite in allSuites) {
            allTestCases.addAll(suite.tests)
            val repeat = 0

            try {
                for (test in suite.tests) {
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
            } catch (ex: Exception) {
                // This should never happen. Added on 31 Jan 2025 trying to chase down a failure
                // in the polyglot steps. Didn't work. Java crashes without throwing a stack trace.
                ex.printStackTrace()
                throw ex
            }

            for (test in suite.tests) {
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
        }
        suiteElapsed = (System.nanoTime() - suiteStart) / 1e9

        val attempted = total - notrun
        val percent = "%.1f".format(100.0 * (1.0 * pass) / (1.0 * attempted))

        statusDir.mkdirs()

        makeReport(lazyLicensedSuite.xmlCalabash)

        println("Test suite: ${pass} pass (${percent}%), ${fail} fail, ${notrun} not run (${total} total)")
        var max = 10
        for (suite in allSuites) {
            for (testCase in suite.tests) {
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
            var firstRegression = true
            var newPass = 0
            var failingDifferently = 0
            for (suite in allSuites) {
                for (testCase in suite.tests) {
                    val status = suite.testCases[testCase]!!
                    if (status.status != "NOTRUN") {
                        val prev = previous[testCase.testFile.toString()]
                        if (prev == null) {
                            if (firstRegression) {
                                println("== Regressions ==========================================================")
                                firstRegression = false
                            }
                            if (status.status == "fail") {
                                println("NEW  ${status} ${testCase.testFile}")
                            }
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
                                    if (firstRegression) {
                                        println("== Regressions ==========================================================")
                                        firstRegression = false
                                    }
                                    println("FAIL ${prev}->${status} ${testCase.testFile}")
                                }
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
            val fos = FileOutputStream(previousStatus)
            val out = PrintStream(fos)
            for (suite in allSuites) {
                for (testCase in suite.tests) {
                    val status = suite.testCases[testCase]!!
                    out.println("${status} ${testCase.testFile}")
                }
            }
            fos.close()
            out.close()
        }

        if (testOptions.requirePass && fail > 0) {
            exitProcess(1)
        }
    }

    fun makeReport(xmlCalabash: XmlCalabash) {
        if (testOptions.report == null) {
            return
        }

        val saxonConfig = xmlCalabash.saxonConfiguration
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

        report.addStartElement(NsReport.testsuite, attributeMap(map))

        report.addStartElement(NsReport.properties)
        val env = saxonConfig.environment
        property(report, "processor", env.productName)
        property(report, "version", env.productVersion)
        property(report, "buildId", env.buildId)
        property(report, "saxonVersion", "${saxonConfig.processor.saxonProductVersion}/${saxonConfig.processor.saxonEdition}")
        property(report, "vendor", env.vendor)
        property(report, "vendorURI", env.vendorUri)
        property(report, "xprocVersion", "3.1")
        property(report, "xpathVersion", env.xpathVersion)
        property(report, "psviSupported", "mixed")
        report.addEndElement()

        for (test in allTestCases) {
            map.clear()
            map[NsReport.name] = test.testFile.name
            map[NsReport.time] = String.format("%1.4f", test.elapsedSeconds)

            report.addStartElement(NsReport.testcase, attributeMap(map))

            if (test.status.status == "skip") {
                report.addStartElement(NsReport.skipped)
                report.addText(test.status.message!!)
                report.addEndElement()
            } else {
                if (test.status.status == "fail") {
                    report.addStartElement(NsReport.failure)
                    if (test.status.error != null) {
                        if (test.status.expectedCodes.isNotEmpty()) {
                            if (test.status.expectedCodes.size == 1) {
                                report.addText("Expected ${test.status.expectedCodes[0]}, raised ${test.status.error!!.code}")
                            } else {
                                report.addText("Expected ${test.status.expectedCodes}, raised ${test.status.error!!.code}")
                            }
                        } else {
                            report.addText("Expected pass, raised ${test.status.error!!.code}")
                        }
                    }
                    report.addEndElement()
                    if (test.messages != null) {
                        report.addSubtree(test.messages!!)
                    }
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

    private fun attributeMap(attr: Map<QName, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(name, value))
            }
        }
        return map
    }

    private fun attributeInfo(name: QName, value: String, location: net.sf.saxon.s9api.Location? = null): AttributeInfo {
        return AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
    }

    private fun fqName(name: QName): FingerprintedQName = FingerprintedQName(name.prefix, name.namespaceUri, name.localName)

    private fun property(report: SaxonTreeBuilder, key: String, value: String) {
        val map = mapOf( NsReport.name to key, NsReport.value to value )
        report.addStartElement(NsReport.property, attributeMap(map))
        report.addEndElement()
    }
}