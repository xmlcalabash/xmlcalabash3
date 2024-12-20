package com.xmlcalabash.steps.validation

import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.prop.rng.RngProperty
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode
import org.xml.sax.InputSource

open class ValidateWithNVDL(): AbstractAtomicStep() {
    lateinit var document: XProcDocument
    lateinit var nvdl: XProcDocument
    val schemas = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        when (port) {
            "source" -> document = doc
            "nvdl" -> nvdl = doc
            else -> schemas.add(doc)
        }
    }

    override fun run() {
        super.run()

        val localDocumentManager = DocumentManager(stepConfig.environment.documentManager)

        val parameters = qnameMapBinding(Ns.parameters)
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val report = Errors(stepConfig, reportFormat)
        val listener = CachingErrorListener(report)
        val properties = PropertyMapBuilder()
        properties.put(ValidateProperty.ERROR_HANDLER, listener)
        RngProperty.CHECK_ID_IDREF.add(properties)

        properties.put(ValidateProperty.ENTITY_RESOLVER, localDocumentManager)

        val srcdoc = document.value as XdmNode
        val nvdldoc = nvdl.value as XdmNode

        for (schema in schemas) {
            localDocumentManager.cache(schema)
        }

        val driver = ValidationDriver(properties.toPropertyMap())

        val nvdl: InputSource = S9Api.xdmToInputSource(stepConfig, nvdldoc)
        val doc: InputSource = S9Api.xdmToInputSource(stepConfig, srcdoc)

        try {
            driver.loadSchema(nvdl)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcNotNvdl(ex.message ?: ""), ex)
        }

        try {
            if (!driver.validate(doc)) {
                if (assertValid) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidNVDL())
                }
            }
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcNotSchemaValidNVDL(), ex)
        }

        receiver.output("result", document)
        receiver.output("report", XProcDocument.ofXml(report.endErrors(), stepConfig))
    }

    override fun toString(): String = "p:identity"
}