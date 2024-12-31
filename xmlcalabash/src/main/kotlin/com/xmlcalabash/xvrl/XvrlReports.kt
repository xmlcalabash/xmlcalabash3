package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsSvrl
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import java.net.URI

class XvrlReports private constructor(stepConfig: XProcStepConfiguration): XvrlElement(stepConfig) {
    lateinit var _metadata: XvrlReportMetadata
    val metadata: XvrlReportMetadata
        get() = _metadata
    val report = mutableListOf<XvrlReport>()
    val reports = mutableListOf<XvrlReports>()
    internal var digest = XvrlDigest(stepConfig)

    companion object {
        val _document = QName("document")
        val _documents = QName("documents")
        val _role = QName("role")
        val _location = QName("location")
        val _context = QName("context")

        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName,String?> = emptyMap()): XvrlReports {
            val reports = XvrlReports(stepConfig)
            reports.commonAttributes(attr)
            reports._metadata = XvrlReportMetadata.newInstance(stepConfig)
            return reports
        }

        fun newInstance(stepConfig: XProcStepConfiguration, metadata: XvrlReportMetadata, attr: Map<QName,String?> = emptyMap()): XvrlReports {
            val reports = XvrlReports(stepConfig)
            reports.commonAttributes(attr)
            reports._metadata = metadata
            return reports
        }

        fun fromSvrl(stepConfig: XProcStepConfiguration, svrl: XdmNode): XvrlReports {
            val reports = newInstance(stepConfig)
            reports.fromSvrl(svrl)
            return reports
        }
    }

    fun report(attr: Map<QName,String?> = emptyMap()): XvrlReport {
        val rep = XvrlReport.newInstance(stepConfig, attr)
        report.add(rep)
        return rep
    }

    fun report(metadata: XvrlReportMetadata, attr: Map<QName,String?> = emptyMap()): XvrlReport {
        val rep = XvrlReport.newInstance(stepConfig, metadata, attr)
        report.add(rep)
        return rep
    }

    fun reports(attr: Map<QName,String?> = emptyMap()): XvrlReports {
        val rep = XvrlReports.newInstance(stepConfig, attr)
        reports.add(rep)
        return rep
    }

    fun reports(metadata: XvrlReportMetadata, attr: Map<QName,String?> = emptyMap()): XvrlReports {
        val rep = XvrlReports.newInstance(stepConfig, metadata, attr)
        reports.add(rep)
        return rep
    }

    fun asXml(baseUri: URI? = null): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseUri)
        serialize(builder)
        builder.endDocument()
        return builder.result
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        if (reports.isEmpty() && report.size == 1 && metadata.isEffectivelyEmpty()) {
            // If a "reports" is a single "report" and there's no interesting metadata
            // associated with the "reports", just serialize the single "report"
            report.first().serialize(builder)
            return
        }

        val digest = XvrlDigest(stepConfig)
        digest.valid = "undetermined"

        builder.addStartElement(NsXvrl.reports, stepConfig.attributeMap(attributes))
        metadata.serialize(builder)
        for (item in reports) {
            item.serialize(builder)
            when (item.digest.valid) {
                "true" -> {
                    if (digest.valid == "undetermined") {
                        digest.valid = "true"
                    }
                }
                "false" -> digest.valid = "false"
                "partial" -> {
                    if (digest.valid == "true") {
                        digest.valid = "partial"
                    }
                }
                "undetermined" -> {
                    if (digest.valid == "true") {
                        digest.valid = "partial"
                    }
                }
            }
            digest.fatalErrorCount += item.digest.fatalErrorCount
            digest.errorCount += item.digest.errorCount
            digest.warningCount += item.digest.warningCount
            digest.infoCount += item.digest.infoCount
            digest.unspecifiedCount += item.digest.unspecifiedCount
            digest.fatalErrorCodes.addAll(item.digest.fatalErrorCodes)
            digest.errorCodes.addAll(item.digest.errorCodes)
            digest.warningCodes.addAll(item.digest.warningCodes)
            digest.infoCodes.addAll(item.digest.infoCodes)
            digest.unspecifiedCodes.addAll(item.digest.unspecifiedCodes)
            digest.worst = item.digest.worst
        }
        for (item in report) {
            item.serialize(builder)
            when (item.digest.valid) {
                "true" -> {
                    if (digest.valid == "unspecified") {
                        digest.valid = "true"
                    }
                }
                "false" -> digest.valid = "false"
                "partial" -> {
                    if (digest.valid == "true") {
                        digest.valid = "partial"
                    }
                }
                "unspecified" -> {
                    if (digest.valid == "true") {
                        digest.valid = "partial"
                    }
                }
            }
            digest.fatalErrorCount += item.digest.fatalErrorCount
            digest.errorCount += item.digest.errorCount
            digest.warningCount += item.digest.warningCount
            digest.infoCount += item.digest.infoCount
            digest.unspecifiedCount += item.digest.unspecifiedCount
            digest.fatalErrorCodes.addAll(item.digest.fatalErrorCodes)
            digest.errorCodes.addAll(item.digest.errorCodes)
            digest.warningCodes.addAll(item.digest.warningCodes)
            digest.infoCodes.addAll(item.digest.infoCodes)
            digest.unspecifiedCodes.addAll(item.digest.unspecifiedCodes)
            digest.worst = item.digest.worst
        }
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

    private fun reportFromSvrl(reports: XvrlReports, svrl: XdmNode) {
        val report = reports.report()
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
        for (node in svrl.axisIterator(Axis.CHILD)) {
            when (node.nodeName) {
                NsSvrl.activePattern -> {
                    val documents = node.getAttributeValue(_documents) ?: node.getAttributeValue(_document)
                    documents?.let { metadata.document(URI.create(it)) }
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