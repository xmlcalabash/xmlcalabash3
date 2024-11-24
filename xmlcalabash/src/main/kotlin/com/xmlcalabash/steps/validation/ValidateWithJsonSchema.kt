package com.xmlcalabash.steps.validation

import com.networknt.schema.*
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

open class ValidateWithJsonSchema(): AbstractAtomicStep() {
    lateinit var document: XProcDocument
    lateinit var schema: XProcDocument

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            document = doc
        } else {
            schema = doc
        }
    }

    override fun run() {
        super.run()

        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val defaultVersion = stringBinding(Ns.defaultVersion)
        val parameters = qnameMapBinding(Ns.parameters)
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"

        if (reportFormat != "xvrl") {
            throw XProcError.xcUnsupportedReportFormat(reportFormat).exception()
        }

        val jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        val config = SchemaValidatorsConfig()
        config.pathType = PathType.JSON_POINTER
        config.isEcma262Validator = true

        val jserializer = XProcSerializer(stepConfig)
        var jbytes = ByteArrayOutputStream()
        jserializer.write(schema, jbytes)
        val jschema = jbytes.toString(StandardCharsets.UTF_8)

        val schema = try {
            jsonSchemaFactory.getSchema(jschema)
        } catch (ex: Exception) {
            if (schema.baseURI == null) {
                throw XProcError.xcJsonSchemaInvalid().exception(ex)
            }
            throw XProcError.xcJsonSchemaInvalid(schema.baseURI!!).exception(ex)
        }

        jbytes = ByteArrayOutputStream()
        jserializer.write(document, jbytes)
        val jinput = jbytes.toString(StandardCharsets.UTF_8)

        val assertions = schema.validate(jinput, InputFormat.JSON) {
            executionContext: ExecutionContext -> executionContext.executionConfig.formatAssertionsEnabled = true
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsXvrl.report)
        for (assertion in assertions) {
            val amap = mapOf(Ns.code to assertion.code, Ns.message to assertion.message)
            builder.addStartElement(NsXvrl.detection, stepConfig.attributeMap(amap))
            builder.addEndElement()
        }
        builder.addEndElement()
        builder.endDocument()

        if (assertions.isEmpty()) {
            receiver.output("result", document)
            receiver.output("report", XProcDocument.ofXml(builder.result, stepConfig))
            return
        }

        if (assertValid) {
            if (document.baseURI == null) {
                throw XProcError.xcNotSchemaValidJson().exception()
            }
            throw XProcError.xcNotSchemaValidJson(document.baseURI!!).exception()
        }

        receiver.output("result", document)
        receiver.output("report", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "p:validate-with-json-schema"
}