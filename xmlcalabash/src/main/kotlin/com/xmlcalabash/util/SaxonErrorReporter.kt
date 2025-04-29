package com.xmlcalabash.util

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import kotlin.collections.set

class SaxonErrorReporter(val stepConfig: StepConfiguration): ErrorReporter {
    private var _error: XmlProcessingError? = null
    val error: XmlProcessingError?
        get() = _error

    private var _errorMessages = mutableListOf<Report>()
    val errorMessages: List<Report>
        get() = _errorMessages

    override fun report(error: XmlProcessingError?) {
        if (error == null) {
            stepConfig.error({ "Saxon error reporter called with null error?"} )
            return
        }

        if (!error.isWarning && _error == null) {
            _error = error
        }

        val report = Report(Verbosity.DEBUG, stepConfig, error)

        _errorMessages.add(report)

        stepConfig.messageReporter.report(Verbosity.DEBUG) { report }
    }
}