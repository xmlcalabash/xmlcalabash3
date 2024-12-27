package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class LoggingMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        when (verbosity) {
            Verbosity.ERROR -> logger.error(message)
            Verbosity.WARN -> logger.warn(message)
            Verbosity.INFO -> logger.info(message)
            Verbosity.PROGRESS -> logger.debug(message)
            Verbosity.DEBUG -> logger.debug(message)
            Verbosity.TRACE -> logger.trace(message)
        }
        nextReporter?.report(verbosity, extraAttributes, message)
    }
}
