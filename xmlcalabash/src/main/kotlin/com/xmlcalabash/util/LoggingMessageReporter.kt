package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.util.NopMessageReporter
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class LoggingMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(severity: Verbosity, report: () -> Report) {
        when (severity) {
            Verbosity.ERROR -> logger.error(report().message)
            Verbosity.WARN -> logger.warn(report().message)
            Verbosity.INFO -> logger.info(report().message)
            Verbosity.DEBUG -> logger.debug(report().message)
            Verbosity.TRACE -> logger.trace(report().message)
        }
        nextReporter?.report(severity, report)
    }
}
