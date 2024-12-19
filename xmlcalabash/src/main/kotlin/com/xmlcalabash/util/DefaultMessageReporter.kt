package com.xmlcalabash.util

class DefaultMessageReporter(initialThreshold: Verbosity): MessageReporter {
    override var threshold = initialThreshold

    override fun warn(message: () -> String) {
        if (threshold <= Verbosity.WARN) {
            println(message())
        }
    }

    override fun info(message: () -> String) {
        if (threshold <= Verbosity.INFO) {
            println(message())
        }
    }

    override fun progress(message: () -> String) {
        if (threshold <= Verbosity.PROGRESS) {
            println(message())
        }
    }

    override fun detail(message: () -> String) {
        if (threshold <= Verbosity.DEBUG) {
            println(message())
        }
    }
}
