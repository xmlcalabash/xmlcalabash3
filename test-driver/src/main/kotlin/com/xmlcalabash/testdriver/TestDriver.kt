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
    var total = 0
    var pass = 0
    var fail = 0
    var notrun = 0
    val testResults = mutableListOf<TestStatus>()
    var suiteElapsed: Double = -1.0

    fun run() {
        val buildDir = File("build")
        buildDir.mkdirs()

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

        val modulus = Math.max(allTests.size / 10, 10)
        val suiteStart = System.nanoTime()

        var width = 0
        for (testFile in allTests) {
            var case = loadTest(lazyLicensed, testFile)
            if (case.features.contains("no-psvi-support")) {
                if (case.features.contains("eager-eval")) {
                    case = loadTest(eagerUnlicensed, testFile)
                } else {
                    case = loadTest(lazyUnlicensed, testFile)
                }
            } else {
                if (case.features.contains("eager-eval")) {
                    case = loadTest(eagerLicensed, testFile)
                }
            }

            if (testFile.absolutePath.contains("/tests/")) {
                val pos = testFile.absolutePath.indexOf("/tests/")
                if (testOptions.consoleOutput) {
                    val message = "\r…/${testFile.absolutePath.substring(pos+7)}"
                    width = message.length
                    print("${message}\r")
                } else {
                    println("…/${testFile.absolutePath.substring(pos+7)}")
                }
            } else {
                width = testFile.absolutePath.length
                if (testOptions.consoleOutput) {
                    print("${testFile.absolutePath}\r")
                } else {
                    println(testFile.absolutePath)
                }
            }

            total++
            case.run()
            testResults.add(case.status)

            if (testOptions.consoleOutput && width > 0) {
                print("\r".padStart(width, ' '))
            }

            when (case.status.status) {
                "pass" -> pass++
                "fail" -> {
                    fail++
                    if (testOptions.stopOnFirstFailed) {
                        break
                    }
                }
                else -> notrun++
            }

            if (testOptions.consoleOutput && (total % modulus == 0)) {
                suiteElapsed = (System.nanoTime() - suiteStart) / 1e9
                val percent = "%.1f".format((100.0 * total) / (1.0 * allTests.size))
                val tpt = suiteElapsed / total
                val remaining = (allTests.size - total) * tpt

                val tptString = "%.3f".format(tpt)
                val elapsedString = "%.1f".format(suiteElapsed)
                val remainingString = "%.1f".format(remaining)
                println("${total} tests (${percent}%) in ${elapsedString}s (${tptString}s/test), estimating ${remainingString}s remain")
            }
        }

        for ((testFile, reason) in skipTests) {
            testResults.add(TestStatus(testFile, "skip", reason))
        }

        suiteElapsed = (System.nanoTime() - suiteStart) / 1e9

        val attempted = total - notrun
        val percent = "%.1f".format(100.0 * (1.0 * pass) / (1.0 * attempted))

        makeReport(lazyLicensed)

        println("Test suite: ${pass} pass (${percent}%), ${fail} fail, ${notrun} not run (${total} total)")

        var max = 10
        for (status in testResults) {
            if (max > 0 && status.status == "fail") {
                max--
                if (status.error != null && status.expectedCodes.isNotEmpty()) {
                    println("FAIL: ${status.testFile.name} (${status.error.code} ≠ ${status.expectedCodes[0]})")
                } else if (status.failedAssertions.isNotEmpty()) {
                    println("FAIL: ${status.testFile.name}: ${status.failedAssertions[0].stringValue}${if (status.failedAssertions.size > 1) " ..." else ""}")
                } else {
                    println("FAIL: ${status.testFile.name}")
                }
            }
        }

        if (testOptions.requirePass && fail > 0) {
            exitProcess(1)
        }
    }

    private fun loadTest(xmlCalabash: XmlCalabash, testFile: File): TestCase {
        val case = TestCase(xmlCalabash, testOptions, testFile)
        case.load()
        return case
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

        for (test in testResults) {
            map.clear()
            map[NsReport.name] = test.testFile.name
            map[NsReport.time] = String.format("%1.4f", test.elapsed)

            report.addStartElement(NsReport.testcase, attributeMap(map))

            if (test.status == "skip") {
                report.addStartElement(NsReport.skipped)
                report.addText(test.message!!)
                report.addEndElement()
            } else {
                if (test.status == "fail") {
                    report.addStartElement(NsReport.failure)
                    if (test.error != null) {
                        if (test.expectedCodes.isNotEmpty()) {
                            if (test.expectedCodes.size == 1) {
                                report.addText("Expected ${test.expectedCodes[0]}, raised ${test.error.code}")
                            } else {
                                report.addText("Expected ${test.expectedCodes}, raised ${test.error.code}")
                            }
                        } else {
                            report.addText("Expected pass, raised ${test.error.code}")
                        }
                    }
                    report.addEndElement()
                    if (test.messagesXml != null) {
                        report.addSubtree(test.messagesXml!!)
                    }
                }

                if (test.stdOutput != null) {
                    report.addStartElement(NsReport.systemOut)
                    report.addText(test.stdOutput!!)
                    report.addEndElement()
                }

                if (test.stdError != null) {
                    report.addStartElement(NsReport.systemErr)
                    report.addText(test.stdError!!)
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