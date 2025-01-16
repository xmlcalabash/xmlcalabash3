package com.xmlcalabash.steps.validation

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SchematronImpl
import com.xmlcalabash.xvrl.XvrlReport
import com.xmlcalabash.xvrl.XvrlReports
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.Xslt30Transformer
import org.xml.sax.InputSource
import javax.xml.transform.sax.SAXSource

open class ValidateWithSchematron(): AbstractAtomicStep() {
    companion object {
        private val s_schematron = QName(NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), "s:schema")
        private val _phase = QName("phase")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val schema = queues["schema"]!!.first()

        val parameters = qnameMapBinding(Ns.parameters)
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val phase = stringBinding(_phase)
        val reportFormat = stringBinding(Ns.reportFormat) ?: "svrl"

        if (reportFormat != "svrl" && reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val impl = SchematronImpl(stepConfig)
        // FIXME: handle parameters

        val tron = S9Api.documentElement(schema.value as XdmNode)
        if (tron.nodeName != s_schematron) {
            throw stepConfig.exception(XProcError.xcNotSchematronSchema(tron.nodeName))
        }

        var report = impl.report(document.value as XdmNode, schema.value as XdmNode, phase)
        val failed = impl.failedAssertions(report)

        if (reportFormat == "xvrl") {
            report = xvrlReport(report, reportFormat, schema)
        }

        if (assertValid) {
            if (failed.isNotEmpty()) {
                val xvrl = xvrlReport(report, reportFormat, schema)
                val doc = XProcDocument.ofXml(xvrl, document.context)
                if (document.baseURI == null) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc))
                }
                throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc, document.baseURI!!))
            }
        }

        receiver.output("report", XProcDocument.ofXml(report, stepConfig))
        receiver.output("result", document)
    }

    private fun xvrlReport(report: XdmNode, reportFormat: String, schema: XProcDocument): XdmNode {
        if (reportFormat == "xvrl") {
            return report
        }

        val xvrl = XvrlReports.fromSvrl(stepConfig, report)
        xvrl.metadata.validator("SchXslt2", XmlCalabashBuildConfig.DEPENDENCIES["schxslt2"] ?: "unknown")

        if (stepConfig.baseUri != null && schema.baseURI != null
            && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())
            && schema.value is XdmNode) {
            // It looks like this one was inline...
            xvrl.metadata.schema(schema.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), null, schema.value as XdmNode)
        } else {
            xvrl.metadata.schema(schema.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"))
        }

        return xvrl.asXml()
    }

    override fun toString(): String = "p:validate-with-schematron"
}