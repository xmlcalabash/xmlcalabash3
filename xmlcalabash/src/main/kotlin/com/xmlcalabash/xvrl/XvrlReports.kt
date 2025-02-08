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
    var digest = XvrlDigest(stepConfig)

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
}