package com.xmlcalabash.steps.validation

import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.auto.AutoSchemaReader
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.StringReader

open class ValidateWithRelaxNG(): AbstractAtomicStep() {
    companion object {
        private val dtdAttributeValues = QName("dtd-attribute-values")
        private val dtdIdIdrefWarnings = QName("dtd-id-idref-warnings")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val schema = queues["schema"]!!.first()

        val compact = schema.contentClassification == MediaClassification.TEXT

        val dtdAttributeValues = booleanBinding(dtdAttributeValues) ?: false
        val dtdIdIdRefWarnings = booleanBinding(dtdIdIdrefWarnings) ?: false
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"
        val parameters = qnameMapBinding(Ns.parameters)

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val report = Errors(stepConfig, document.baseURI)
        report.report.metadata.validator("Jing", XmlCalabashBuildConfig.DEPENDENCIES["jing"] ?: "unknown")

        val language = if (compact) "RNC" else "RNG"
        if (stepConfig.baseUri != null && schema.baseURI != null
            && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())
            && schema.value is XdmNode) {
            // It looks like this one was inline...
            report.report.metadata.schema(schema.baseURI, NamespaceUri.of("http://relaxng.org/ns/structure/1.0"), language, null, schema.value as XdmNode)
        } else {
            report.report.metadata.schema(schema.baseURI, NamespaceUri.of("http://relaxng.org/ns/structure/1.0"), language)
        }

        val listener = CachingErrorListener(stepConfig, report)
        val properties = PropertyMapBuilder()
        properties.put(ValidateProperty.ERROR_HANDLER, listener)
        //properties.put(ValidateProperty.URI_RESOLVER, ...)
        //properties.put(ValidateProperty.ENTITY_RESOLVER, ...)

        if (dtdIdIdRefWarnings) {
            RngProperty.CHECK_ID_IDREF.add(properties)
        }

        var sr: SchemaReader? = null
        var schemaInputSource: InputSource? = null

        if (compact) {
            sr = CompactSchemaReader.getInstance()
            // Hack
            val srdr = StringReader(schema.value.underlyingValue.stringValue)
            schemaInputSource = InputSource(srdr)
            schemaInputSource.systemId = schema.baseURI.toString()
        } else {
            sr = AutoSchemaReader()
            schemaInputSource = S9Api.xdmToInputSource(stepConfig, schema)
        }

        var except: XProcError? = null
        var errors: XdmNode? = null
        val driver = ValidationDriver(properties.toPropertyMap(), sr)

        val loaded = try {
            driver.loadSchema(schemaInputSource)
        } catch (ex: Exception) {
            if (document.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNotRelaxNG("Error loading schema"), ex)
            }
            throw stepConfig.exception(XProcError.xcNotRelaxNG(document.baseURI!!, "Error loading schema"), ex)
        }

        if (!loaded) {
            if (document.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNotRelaxNG("Error loading schema"))
            }
            throw stepConfig.exception(XProcError.xcNotRelaxNG(document.baseURI!!, "Error loading schema"))
        }

        val din = S9Api.xdmToInputSource(stepConfig, document)
        if (!driver.validate(din)) {
            val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)
            if (assertValid) {
                throw stepConfig.exception(XProcError.xcNotSchemaValidRelaxNG(xvrl))
            }
        }

        receiver.output("result", document)
        receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig))
    }

    override fun toString(): String = "p:validate-with-relax-ng"
}