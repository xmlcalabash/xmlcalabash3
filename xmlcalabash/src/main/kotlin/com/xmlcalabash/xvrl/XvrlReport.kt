package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.HostLanguage
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import org.apache.logging.log4j.kotlin.logger
import java.net.URI

class XvrlReport private constructor(stepConfig: XProcStepConfiguration, val metadata: XvrlReportMetadata): XvrlElement(stepConfig) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName,String?> = emptyMap()): XvrlReport {
            val metadata = XvrlReportMetadata.newInstance(stepConfig)
            val report = XvrlReport(stepConfig, metadata)
            report.commonAttributes(attr)
            return report
        }

        fun newInstance(stepConfig: XProcStepConfiguration, metadata: XvrlReportMetadata, attr: Map<QName,String?> = emptyMap()): XvrlReport {
            val report = XvrlReport(stepConfig, metadata)
            report.commonAttributes(attr)
            return report
        }
    }

    var digest = XvrlDigest(stepConfig)
    val detections = mutableListOf<XvrlDetection>()

    fun detection(severity: String, code: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlDetection {
        val detection = XvrlDetection.newInstance(stepConfig, severity, code, attr)
        detections.add(detection)
        return detection
    }

    fun detection(severity: String, code: String?, message: String, attr: Map<QName,String?> = emptyMap()): XvrlDetection {
        val detection = XvrlDetection.newInstance(stepConfig, severity, code, attr)
        detection.message.add(XvrlMessage.newInstance(stepConfig, message))
        detections.add(detection)
        return detection
    }

    fun detection(error: XmlProcessingError): XvrlDetection {
        val severity = if (error.isWarning) {
            "warning"
        } else {
            "error"
        }

        val extra = mutableMapOf<QName, String>()
        if (error.hostLanguage != HostLanguage.UNKNOWN) {
            extra[NsSaxon.hostLanguage] = "${error.hostLanguage}".lowercase()
        }
        if (error.isStaticError) {
            extra[NsSaxon.static] = "${error.isStaticError}"
        }
        if (error.isTypeError) {
            extra[NsSaxon.type] = "${error.isTypeError}"
        }
        if (error.isAlreadyReported) {
            extra[NsSaxon.alreadyReported] = "${error.isAlreadyReported}"
        }
        error.failingExpression?.let { extra[NsSaxon.expression] = "${error.failingExpression}" }
        error.terminationMessage?.let { extra[NsSaxon.terminationMessage] = it }

        val message = if (error.cause is XPathException && error.cause.message != null) {
            error.cause.message!!
        } else {
            error.message
        }

        val detection = if (error.errorCode != null) {
            detection(severity, "${error.errorCode}", message.trim(), extra)
        } else {
            detection(severity, null, message.trim(), extra)
        }

        if (error.location.systemId != null && error.location.systemId != "") {
            val loc = detection.location(com.xmlcalabash.datamodel.Location(error.location))
            error.path?.let { loc.xpath = it }
        }

        return detection
    }

    fun asXml(baseUri: URI? = null): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseUri)
        serialize(builder)
        builder.endDocument()
        return builder.result
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        val digest = XvrlDigest(stepConfig)
        builder.addStartElement(NsXvrl.report, stepConfig.attributeMap(attributes))
        metadata.serialize(builder)
        var overall = "true"
        for (item in detections) {
            item.serialize(builder)
            digest.worst = item.severity
            when (item.severity) {
                "info" -> {
                    digest.infoCount++
                    item.code?.let { digest.infoCodes.add(it) }
                }
                "warning" -> {
                    digest.warningCount++
                    item.code?.let { digest.warningCodes.add(it) }
                    if (overall == "true") {
                        overall = "partial"
                    }
                }
                "error" -> {
                    digest.errorCount++
                    item.code?.let { digest.errorCodes.add(it) }
                    overall = "false"
                }
                "fatal-error" -> {
                    digest.fatalErrorCount++
                    item.code?.let { digest.fatalErrorCodes.add(it) }
                    overall = "false"
                }
                "unspecified" -> {
                    digest.unspecifiedCount++
                    item.code?.let { digest.unspecifiedCodes.add(it) }
                    overall = "undetermined"
                }
                else -> logger.warn("Unexpected severity ${item.severity}")
            }
        }
        digest.valid = overall
        digest.serialize(builder)
        builder.addEndElement()
    }
}