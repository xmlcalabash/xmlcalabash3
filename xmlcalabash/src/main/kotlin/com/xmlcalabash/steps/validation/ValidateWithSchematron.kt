package com.xmlcalabash.steps.validation

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SchematronImpl
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

    lateinit var document: XProcDocument
    lateinit var schema: XProcDocument
    private var svrlToXvrl: Xslt30Transformer? = null

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            document = doc
        } else {
            schema = doc
        }
    }

    override fun run() {
        super.run()

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
            if (svrlToXvrl == null) {
                var styleStream = SaxonConfiguration::class.java.getResourceAsStream("/com/xmlcalabash/svrl2xvrl.xsl")
                var styleSource = SAXSource(InputSource(styleStream))
                var xsltCompiler = stepConfig.processor.newXsltCompiler()
                var xsltExec = xsltCompiler.compile(styleSource)
                svrlToXvrl = xsltExec.load30()
            }
            val destination = XdmDestination()
            svrlToXvrl!!.transform(report.asSource(), destination)
            report = destination.xdmNode
        }

        if (assertValid) {
            if (failed.isNotEmpty()) {
                val doc = XProcDocument.ofXml(report, document.context)
                if (document.baseURI == null) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc))
                }
                throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc, document.baseURI!!))
            }
        }

        receiver.output("report", XProcDocument.ofXml(report, stepConfig))
        receiver.output("result", document)
    }

    override fun toString(): String = "p:validate-with-schematron"
}