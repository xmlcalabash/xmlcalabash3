package com.xmlcalabash.steps.validation

import com.networknt.schema.*
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.XdmNode
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

open class ValidateWithJsonSchema(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val schema = queues["schema"]!!.first()

        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val defaultVersion = stringBinding(Ns.defaultVersion)
        val parameters = qnameMapBinding(Ns.parameters)
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

        var jbytes = ByteArrayOutputStream()
        DocumentWriter(schema, jbytes).write()
        val jschema = jbytes.toString(StandardCharsets.UTF_8)

        val jsonSchema = try {
            jsonSchemaFactory.getSchema(jschema)
        } catch (ex: Exception) {
            val message = ex.message ?: ""
            val err = if (schema.baseURI == null) {
                if (message.contains("UnsupportedOperationException")) {
                    XProcError.xcJsonSchemaInvalid()
                } else {
                    XProcError.xcJsonSchemaNotSupported()
                }
            } else {
                if (message.contains("UnsupportedOperationException")) {
                    XProcError.xcJsonSchemaInvalid(schema.baseURI!!)
                } else {
                    XProcError.xcJsonSchemaNotSupported(schema.baseURI!!)
                }
            }
            throw err.exception(ex)
        }

        jbytes = ByteArrayOutputStream()
        DocumentWriter(document, jbytes).write()
        val jinput = jbytes.toString(StandardCharsets.UTF_8)

        val assertions = jsonSchema.validate(jinput, InputFormat.JSON) {
            executionContext: ExecutionContext -> executionContext.executionConfig.formatAssertionsEnabled = true
        }

        val report = XvrlReport.newInstance(stepConfig)
        report.metadata.creator(stepConfig.saxonConfig.environment.productName,
            stepConfig.saxonConfig.environment.productVersion)
        report.metadata.validator("jsonSchemaValidator", XmlCalabashBuildConfig.DEPENDENCIES["jsonSchemaValidator"] ?: "unknown")

        if (stepConfig.baseUri != null && schema.baseURI != null
            && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())
            && schema.value is XdmNode) {
            // It looks like this one was inline...
            report.metadata.schema(schema.baseURI, NamespaceUri.of("https://json-schema.org/draft/2020-12/schema"), null, schema.value as XdmNode)
        } else {
            report.metadata.schema(schema.baseURI, NamespaceUri.of("https://json-schema.org/draft/2020-12/schema"))
        }

        document.baseURI?.let { report.metadata.document(it) }
        for (assertion in assertions) {
            val detection = report.detection("error", assertion.code, assertion.message)
            if (assertion.schemaLocation != null || assertion.instanceLocation != null) {
                val uri = if (assertion.schemaLocation != null) {
                    URI("${assertion.schemaLocation}")
                } else {
                    null
                }
                val loc = detection.location(uri)
                if (assertion.instanceLocation != null) {
                    loc.jsonpath = "${assertion.instanceLocation}"
                }
            }
        }

        if (assertions.isEmpty()) {
            receiver.output("result", document)
            receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig))
            return
        }

        if (assertValid) {
            if (document.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNotSchemaValidJson())
            }
            throw stepConfig.exception(XProcError.xcNotSchemaValidJson(document.baseURI!!))
        }

        receiver.output("result", document)
        receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig))
    }

    override fun toString(): String = "p:validate-with-json-schema"
}