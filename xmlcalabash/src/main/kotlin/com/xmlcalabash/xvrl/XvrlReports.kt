package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlReports private constructor(stepConfig: XProcStepConfiguration): XvrlElement(stepConfig) {
    lateinit var _metadata: XvrlReportMetadata
    val metadata: XvrlReportMetadata = _metadata
    val report = mutableListOf<XvrlReport>()
    val reports = mutableListOf<XvrlReports>()
    internal var digest = XvrlDigest(stepConfig)

    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName, String> = emptyMap()): XvrlReports {
            val reports = XvrlReports(stepConfig)
            reports.commonAttributes(attr)
            reports._metadata = XvrlReportMetadata.newInstance(stepConfig)
            return reports
        }

        fun newInstance(stepConfig: XProcStepConfiguration, metadata: XvrlReportMetadata, attr: Map<QName, String> = emptyMap()): XvrlReports {
            val reports = XvrlReports(stepConfig)
            reports.commonAttributes(attr)
            reports._metadata = metadata
            return reports
        }
    }

    fun report(attr: Map<QName, String> = emptyMap()): XvrlReport {
        val rep = XvrlReport.newInstance(stepConfig, attr)
        report.add(rep)
        return rep
    }

    fun report(metadata: XvrlReportMetadata, attr: Map<QName, String> = emptyMap()): XvrlReport {
        val rep = XvrlReport.newInstance(stepConfig, metadata, attr)
        report.add(rep)
        return rep
    }

    fun reports(attr: Map<QName, String> = emptyMap()): XvrlReports {
        val rep = XvrlReports.newInstance(stepConfig, attr)
        reports.add(rep)
        return rep
    }

    fun reports(metadata: XvrlReportMetadata, attr: Map<QName, String> = emptyMap()): XvrlReports {
        val rep = XvrlReports.newInstance(stepConfig, metadata, attr)
        reports.add(rep)
        return rep
    }

    fun serialize(baseUri: URI? = null): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseUri)
        serialize(builder)
        builder.endDocument()
        return builder.result
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        val digest = XvrlDigest(stepConfig)
        digest.valid = "unspecified"

        builder.addStartElement(NsXvrl.reports, stepConfig.attributeMap(attributes))
        for (item in reports) {
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