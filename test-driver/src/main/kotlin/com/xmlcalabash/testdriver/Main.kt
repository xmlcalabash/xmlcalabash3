package com.xmlcalabash.testdriver

import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            val testOptions = TestOptions()

            /*
            println("PID: ${ProcessHandle.current().pid()}")
            for (count in 0..9) {
                println(10 - count)
                Thread.sleep(1000)
            }
             */

            for (arg in args) {
                if (arg.startsWith("-t:")) {
                    testOptions.testRegex = arg.substring(3)
                } else if (arg.startsWith("--title:")) {
                    testOptions.title = arg.substring(8)
                } else if (arg.startsWith("--graph:")) {
                    testOptions.outputGraph = arg.substring(8)
                } else if (arg == "--stop-on-fail") {
                    testOptions.stopOnFirstFailed = true
                } else if (arg.startsWith("--require-pass:")) {
                    testOptions.requirePass = (arg.substring(15) == "true")
                } else if (arg.startsWith("--report:")) {
                    testOptions.report = arg.substring(9)
                } else if (arg.startsWith("--dir:")) {
                    testOptions.testDirectoryList.add(arg.substring(6))
                } else if (arg.startsWith("--console:")) {
                    testOptions.consoleOutput = (arg.substring(10) == "true")
                } else {
                    throw RuntimeException("Unrecognized argument: ${arg}")
                }
            }

            // The default test suite...
            if (testOptions.testDirectoryList.isEmpty()) {
                testOptions.testDirectoryList.add("../tests/3.0-test-suite/test-suite/tests")
                testOptions.testDirectoryList.add("../tests/extra-suite/test-suite/tests")
            }

            val exclusions = mutableMapOf<String, String>()
            File("src/test/resources/exclusions.txt").forEachLine { line ->
                val pos = line.trim().indexOf(" because ")
                val filename = if (pos >= 0) {
                    line.substring(0, pos).trim()
                } else {
                    line.trim()
                }
                if (filename.isNotEmpty()) {
                    val reason = line.substring(pos + 9).trim()
                    exclusions[filename] = reason
                }
            }

            val testDriver = TestDriver(testOptions, exclusions)
            //testDriver.threadTest()
            testDriver.run()
        }
    }
}
