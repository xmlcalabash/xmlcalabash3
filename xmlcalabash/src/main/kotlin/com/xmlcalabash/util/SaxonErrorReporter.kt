package com.xmlcalabash.util

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import kotlin.collections.set

class SaxonErrorReporter(val stepConfig: XProcStepConfiguration): ErrorReporter {
    companion object {
        val _systemIdentifier = QName("system-identifier")
        val _publicIdentifier = QName("public-identifier")

        val saxonNamespace = NamespaceUri.of("http://saxon.sf.net/")
        val s_hostLanguage = QName(saxonNamespace, "s:host-language")
        val s_static =QName(saxonNamespace, "s:static")
        val s_type = QName(saxonNamespace, "s:type")
        val s_expression = QName(saxonNamespace, "s:expression")
        val s_terminationMessage = QName(saxonNamespace, "s:termination-message")
        val s_alreadyReported = QName(saxonNamespace, "s:already-reported")
    }

    override fun report(error: XmlProcessingError?) {
        if (error == null) {
            stepConfig.error({ "Saxon error reporter called with null error?"} )
            return
        }

        val extra = mutableMapOf<QName, String>()
        extra[s_hostLanguage] = "${error.hostLanguage}"
        extra[s_static] = "${error.isStaticError}"
        extra[s_type] = "${error.isTypeError}"
        if (error.errorCode != null) {
            extra[Ns.code] = "Q{${error.errorCode.namespaceUri}}${error.errorCode.localName}"
        }

        error.location.publicId?.let { extra[_publicIdentifier] = it }
        if (error.location.systemId != null && error.location.systemId != "") {
            extra[_systemIdentifier] = error.location.systemId
        }
        if (error.location.lineNumber > 0) {
            extra[Ns.lineNumber] = "${error.location.lineNumber}"
        }
        if (error.location.columnNumber > 0) {
            extra[Ns.columnNumber] = "${error.location.columnNumber}"
        }
        error.failingExpression?.let { extra[s_expression] = "${error.failingExpression}" }
        error.path?.let { extra[Ns.path] = it }
        error.terminationMessage?.let { extra[s_terminationMessage] = it }
        if (error.isAlreadyReported) {
            extra[s_alreadyReported] = "${error.isAlreadyReported}"
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