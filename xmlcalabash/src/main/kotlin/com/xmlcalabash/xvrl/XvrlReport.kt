package com.xmlcalabash.xvrl

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsSvrl
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.Xslt30Transformer
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.net.URI
import javax.xml.transform.sax.SAXSource

class XvrlReport private constructor(stepConfig: XProcStepConfiguration, val metadata: XvrlReportMetadata): XvrlElement(stepConfig) {
    companion object {
        private var svrlToXvrl: Xslt30Transformer? = null

        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName, String> = emptyMap()): XvrlReport {
            val metadata = XvrlReportMetadata.newInstance(stepConfig)
            val report = XvrlReport(stepConfig, metadata)
            report.commonAttributes(attr)
            return report
        }

        fun newInstance(stepConfig: XProcStepConfiguration, metadata: XvrlReportMetadata, attr: Map<QName, String> = emptyMap()): XvrlReport {
            val report = XvrlReport(stepConfig, metadata)
            report.commonAttributes(attr)
            return report
        }

        fun from(stepConfig: XProcStepConfiguration, svrl: XdmNode): XdmNode {
            val root = if (svrl.nodeKind == XdmNodeKind.DOCUMENT) {
                S9Api.firstElement(svrl)!!
            } else {
                svrl
            }

            if (root.nodeKind != XdmNodeKind.ELEMENT || root.nodeName != NsSvrl.schematronOutput) {
                throw stepConfig.exception(XProcError.xiXvrlInvalidSvrl(root.nodeName))
            }

            if (svrlToXvrl == null) {
                var styleStream = SaxonConfiguration::class.java.getResourceAsStream("/com/xmlcalabash/svrl2xvrl.xsl")
                var styleSource = SAXSource(InputSource(styleStream))
                var xsltCompiler = svrl.processor.newXsltCompiler()
                var xsltExec = xsltCompiler.compile(styleSource)
                svrlToXvrl = xsltExec.load30()
            }
            val destination = XdmDestination()
            svrlToXvrl!!.transform(svrl.asSource(), destination)
            return destination.xdmNode
        }
    }

    internal var digest = XvrlDigest(stepConfig)
    val detections = mutableListOf<XvrlDetection>()

    fun detection(severity: String, code: String? = null, attr: Map<QName,String> = emptyMap()): XvrlDetection {
        val detection = XvrlDetection.newInstance(stepConfig, severity, code, attr)
        detections.add(detection)
        return detection
    }

    fun detection(severity: String, code: String?, message: String, attr: Map<QName,String> = emptyMap()): XvrlDetection {
        val detection = XvrlDetection.newInstance(stepConfig, severity, code, attr)
        detection.message.add(XvrlMessage.newInstance(stepConfig, message))
        detections.add(detection)
        return detection
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