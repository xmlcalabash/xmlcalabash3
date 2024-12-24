package com.xmlcalabash.util

class DefaultMessageReporter(initialThreshold: Verbosity): MessageReporter {
    override var threshold = initialThreshold

    override fun error(message: () -> String) {
        System.err.println(message())
    }

    override fun warn(message: () -> String) {
        if (threshold <= Verbosity.WARN) {
            System.err.println(message())
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

    override fun debug(message: () -> String) {
        if (threshold <= Verbosity.DEBUG) {
            println(message())
        }
    }

    override fun trace(message: () -> String) {
        if (threshold <= Verbosity.TRACE) {
            println(message())
        }
    }
}
