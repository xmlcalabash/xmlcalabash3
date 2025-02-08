package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.namespace.NsSvrl
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.Verbosity
import com.xmlcalabash.xvrl.XvrlReports.Companion._context
import com.xmlcalabash.xvrl.XvrlReports.Companion._document
import com.xmlcalabash.xvrl.XvrlReports.Companion._documents
import com.xmlcalabash.xvrl.XvrlReports.Companion._location
import com.xmlcalabash.xvrl.XvrlReports.Companion._role
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.HostLanguage
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
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

        fun fromSvrl(stepConfig: XProcStepConfiguration, svrl: XdmNode): XvrlReport {
            val report = newInstance(stepConfig)
            report.fromSvrl(svrl)
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
        builder.addStartElement(NsXvrl.report, stepConfig.attributeMap(attributes))
        metadata.serialize(builder)
        digest.clear()
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

    // ============================================================

    private fun fromSvrl(svrl: XdmNode) {
        when (svrl.nodeKind) {
            XdmNodeKind.DOCUMENT -> {
                for (node in svrl.axisIterator(Axis.CHILD)) {
                    fromSvrl(node)
                }
            }

            XdmNodeKind.ELEMENT -> {
                if (svrl.nodeName == NsSvrl.schematronOutput) {
                    reportFromSvrl(this, svrl)
                } else {
                    for (node in svrl.axisIterator(Axis.CHILD)) {
                        fromSvrl(node)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun reportFromSvrl(report: XvrlReport, svrl: XdmNode) {
        report.metadata.schema(null, NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"))
        metadataFromSvrl(report.metadata, svrl)
        val children = mutableListOf<XdmNode>()
        for (node in svrl.axisIterator(Axis.CHILD)) {
            if (node.nodeKind == XdmNodeKind.ELEMENT) {
                children.add(node)
            }
        }

        while (children.isNotEmpty()) {
            val node = children.removeFirst()
            if (node.nodeName == NsSvrl.activePattern) {
                patternFromSvrl(report, node, children)
            }
        }
    }

    private fun metadataFromSvrl(metadata: XvrlReportMetadata, svrl: XdmNode) {
        val seen = mutableSetOf<URI>()
        for (node in svrl.axisIterator(Axis.CHILD)) {
            when (node.nodeName) {
                NsSvrl.activePattern -> {
                    val documents = node.getAttributeValue(_documents) ?: node.getAttributeValue(_document)
                    if (documents != null) {
                        val uri = URI(documents)
                        if (uri !in seen) {
                            metadata.document(uri)
                            seen.add(uri)
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun patternFromSvrl(report: XvrlReport, pattern: XdmNode, children: MutableList<XdmNode>) {
        while (children.isNotEmpty()) {
            if (children.first().nodeName == NsSvrl.activePattern) {
                return
            }
            val node = children.removeFirst()
            if (node.nodeName == NsSvrl.firedRule) {
                ruleFromSvrl(report, pattern, node, children)
            }
        }
    }

    private fun ruleFromSvrl(report: XvrlReport, pattern: XdmNode, rule: XdmNode, children: MutableList<XdmNode>) {
        while (children.isNotEmpty()) {
            if (children.first().nodeName == NsSvrl.firedRule) {
                return
            }
            val node = children.removeFirst()
            when (node.nodeName) {
                NsSvrl.successfulReport -> {
                    detectionFromSvrl(report, pattern, rule, node, "info")
                }
                NsSvrl.failedAssert -> {
                    detectionFromSvrl(report, pattern, rule, node, "error")
                }
                else -> Unit
            }
        }
    }

    private fun detectionFromSvrl(report: XvrlReport, pattern: XdmNode, rule: XdmNode, node: XdmNode, defaultSeverity: String) {
        val severity = node.getAttributeValue(_role) ?: defaultSeverity
        val detection = report.detection(severity)

        if (node.getAttributeValue(_location) != null) {
            val location = detection.location()
            location.xpath = node.getAttributeValue(_location)
        }

        if (rule.getAttributeValue(_context) != null) {
            val context = detection.context()
            val location = context.location()
            location.xpath = rule.getAttributeValue(_context)
        }

        val lang = node.getAttributeValue(NsXml.lang)
        for (child in node.axisIterator(Axis.CHILD)) {
            if (child.nodeName == NsSvrl.text || child.nodeName == NsSvrl.diagnosticReference) {
                messageFromSvrl(detection, child, lang)
            }
        }
    }

    private fun messageFromSvrl(detection: XvrlDetection, node: XdmNode, defaultLanguage: String?) {
        val lang = node.getAttributeValue(NsXml.lang) ?: defaultLanguage
        val atts = mutableMapOf<QName,String>()
        lang?.let { atts[NsXml.lang] = it }
        val message = detection.message(atts)
        for (child in node.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.TEXT -> {
                    val text = child.stringValue.trim()
                    if (text.isNotEmpty()) {
                        message.message(text)
                    }
                }
                XdmNodeKind.ELEMENT -> {
                    val element = message.message(child.nodeName)
                    recurse(element, child)
                }
                else -> Unit
            }
        }
    }

    private fun recurse(message: XvrlMessageElement, node: XdmNode) {
        for (child in node.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.TEXT -> {
                    val text = child.stringValue.trim()
                    if (text.isNotEmpty()) {
                        message.addContent(XvrlText(stepConfig, text))
                    }
                }
                XdmNodeKind.ELEMENT -> {
                    val element = message.message(child.nodeName)
                    recurse(element, child)
                }
                else -> Unit
            }
        }
    }

}