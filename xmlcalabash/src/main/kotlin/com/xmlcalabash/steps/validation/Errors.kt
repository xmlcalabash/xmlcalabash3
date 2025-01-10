package com.xmlcalabash.steps.validation

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.lib.Invalidity
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.ValidationFailure
import org.xml.sax.SAXParseException
import java.net.URI

class Errors(stepConfig: XProcStepConfiguration, uri: URI?) {
    val report = XvrlReport.newInstance(stepConfig)
    init {
        report.metadata.creator(stepConfig.saxonConfig.environment.productName,
            stepConfig.saxonConfig.environment.productVersion)
        uri?.let { report.metadata.document(it) }
    }

    fun asXml(): XdmNode {
        return report.asXml()
    }

    fun xsdValidationError(msg: String, fail: ValidationFailure) {
        val amap = mutableMapOf<QName, String?>(
            NsSaxon.constraintName to fail.constraintName,
            NsSaxon.constraintClause to fail.constraintClauseNumber,
            NsSaxon.schemaType to fail.schemaType?.toString(),
            NsSaxon.schemaPart to if (fail.schemaPart > 0) "${fail.schemaPart}" else null
        )

        val detection = report.detection("error", null, msg, amap)
        fail.errorCodeQName?.let { detection.code = "Q{${it.namespaceUri}}${it.localPart}"}

        if (fail.absolutePath != null || fail.systemId != null) {
            val uri = if (fail.systemId != null) {
                URI(fail.systemId!!)
            } else {
                null
            }
            val loc = detection.location(uri, fail.lineNumber, fail.columnNumber)
            fail.absolutePath?.let { loc.xpath = "${fail.absolutePath}" }
        }
    }

    fun xsdValidationError(fail: Invalidity) {
        val amap = mutableMapOf<QName, String?>(
            NsSaxon.constraintName to fail.constraintName,
            NsSaxon.constraintClause to fail.constraintClauseNumber,
            NsSaxon.constraintReference to fail.constraintReference,
            NsSaxon.schemaPart to if (fail.schemaPart > 0) "${fail.schemaPart}" else null
        )

        val detection = report.detection("error", null, fail.message, amap)
        fail.errorCode?.let { detection.code = it }

        if (fail.path != null || fail.systemId != null) {
            val uri = if (fail.systemId != null) {
                URI(fail.systemId!!)
            } else {
                null
            }
            val loc = detection.location(uri, fail.lineNumber, fail.columnNumber)
            fail.path?.let { loc.xpath = "${fail.path}" }
        }

        if (fail.contextPath != null) {
            val context = detection.context()
            val loc = context.location()
            loc.xpath = "${fail.contextPath}"
        }
    }

    fun xsdValidationError(msg: String) {
        report.detection("error", null, msg)
    }

    fun validationError(msg: String, fail: SAXParseException) {
        val detection = report.detection("error", null, msg)
        if (fail.systemId != null) {
            val amap = mutableMapOf<QName, String?>(
                NsCx.publicIdentifier to fail.publicId
            )
            val uri = URI(fail.systemId!!)
            val loc = detection.location(uri, fail.lineNumber, fail.columnNumber, amap)
        }
    }

    fun jsonValidationError(code: String, message: String) {
        report.detection("error", code, message)
    }
}