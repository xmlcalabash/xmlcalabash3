package com.xmlcalabash.steps.validation

import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.prop.rng.RngProperty
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.XdmNode
import org.xml.sax.InputSource

open class ValidateWithNVDL(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val nvdl = queues["nvdl"]!!.first()
        val schemas = queues["schemas"]!!

        val localDocumentManager = DocumentManager(stepConfig.environment.documentManager)

        val parameters = qnameMapBinding(Ns.parameters)
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val report = Errors(stepConfig, document.baseURI)
        report.report.metadata.validator("Jing", XmlCalabashBuildConfig.DEPENDENCIES["jing"] ?: "unknown")

        val listener = CachingErrorListener(stepConfig, report)
        val properties = PropertyMapBuilder()
        properties.put(ValidateProperty.ERROR_HANDLER, listener)
        RngProperty.CHECK_ID_IDREF.add(properties)

        properties.put(ValidateProperty.ENTITY_RESOLVER, localDocumentManager)

        val srcdoc = document.value as XdmNode
        val nvdldoc = nvdl.value as XdmNode

        if (stepConfig.baseUri != null && nvdldoc.baseURI != null
            && nvdldoc.baseURI.toString().startsWith(stepConfig.baseUri.toString())) {
            // It looks like this one was inline...
            report.report.metadata.schema(nvdldoc.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/nvdl/ns/structure/1.0"), "NVDL", null, nvdldoc)
        } else {
            report.report.metadata.schema(nvdldoc.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/nvdl/ns/structure/1.0"), "NVDL")
        }

        for (schema in schemas) {
            localDocumentManager.cache(schema)
            // TODO: if additional schemas are provided, what type are they? Add them to the metadata.
        }

        val driver = ValidationDriver(properties.toPropertyMap())

        val nvdlSource: InputSource = S9Api.xdmToInputSource(stepConfig, nvdldoc)
        val docSource: InputSource = S9Api.xdmToInputSource(stepConfig, srcdoc)

        try {
            driver.loadSchema(nvdlSource)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcNotNvdl(ex.message ?: ""), ex)
        }

        try {
            if (!driver.validate(docSource)) {
                val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)
                if (assertValid) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidNVDL(xvrl))
                }
            }
        } catch (ex: Exception) {
            val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)
            throw stepConfig.exception(XProcError.xcNotSchemaValidNVDL(xvrl), ex)
        }

        receiver.output("result", document)
        receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig))
    }

    override fun toString(): String = "p:validate-with-nvdl"
}