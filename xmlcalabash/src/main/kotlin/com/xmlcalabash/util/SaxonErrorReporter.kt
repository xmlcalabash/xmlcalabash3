package com.xmlcalabash.util

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import kotlin.collections.set

class SaxonErrorReporter(val stepConfig: XProcStepConfiguration): ErrorReporter {
    override fun report(error: XmlProcessingError?) {
        if (error == null) {
            stepConfig.error({ "Saxon error reporter called with null error?"} )
            return
        }

        val extra = mutableMapOf<QName, String>()
        extra[NsSaxon.hostLanguage] = "${error.hostLanguage}"
        extra[NsSaxon.static] = "${error.isStaticError}"
        extra[NsSaxon.type] = "${error.isTypeError}"
        if (error.errorCode != null) {
            extra[Ns.code] = "Q{${error.errorCode.namespaceUri}}${error.errorCode.localName}"
        }

        error.location.publicId?.let { extra[Ns.publicIdentifier] = it }
        if (error.location.systemId != null && error.location.systemId != "") {
            extra[Ns.systemIdentifier] = error.location.systemId
        }
        if (error.location.lineNumber > 0) {
            extra[Ns.lineNumber] = "${error.location.lineNumber}"
        }
        if (error.location.columnNumber > 0) {
            extra[Ns.columnNumber] = "${error.location.columnNumber}"
        }
        error.failingExpression?.let { extra[NsSaxon.expression] = "${error.failingExpression}" }
        error.path?.let { extra[Ns.path] = it }
        error.terminationMessage?.let { extra[NsSaxon.terminationMessage] = it }
        if (error.isAlreadyReported) {
            extra[NsSaxon.alreadyReported] = "${error.isAlreadyReported}"
        }

        val message = if (error.cause is XPathException && error.cause.message != null) {
            error.cause.message!!
        } else {
            error.message
        }

        if (error.isWarning) {
            stepConfig.environment.messageReporter.report(Verbosity.WARN, extra) { message }
        } else {
            stepConfig.environment.messageReporter.report(Verbosity.ERROR, extra) { message }
        }
    }
}