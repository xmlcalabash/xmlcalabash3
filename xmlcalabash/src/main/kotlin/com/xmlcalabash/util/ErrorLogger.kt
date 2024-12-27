package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.tree.AttributeLocation
import org.apache.logging.log4j.kotlin.logger

class ErrorLogger(val stepConfig: XProcStepConfiguration): ErrorReporter {
    override fun report(error: XmlProcessingError?) {
        if (error == null) {
            return
        }

        if (error.location != null) {
            val sb = StringBuilder()
            if (error.isWarning) {
                sb.append("Warning")
            } else {
                sb.append("Error")
            }
            val loc = error.location
            if (loc is AttributeLocation) {
                sb.append(" in ${loc.elementName}/${loc.attributeName}")
            }
            if (loc != null && loc.lineNumber > 0) {
                sb.append(" on line ${loc.lineNumber}")
                if (loc.columnNumber > 0) {
                    sb.append(" column ${loc.columnNumber}")
                }
            }
            stepConfig.debug { sb.toString() }
        }

        val sb = StringBuilder()
        if (error.location != null) {
            sb.append("  ")
        }
        if (error.errorCode != null) {
            sb.append(error.errorCode.toString()).append(" ")
        }
        sb.append(error.message)
        stepConfig.debug { sb.toString() }
    }
}