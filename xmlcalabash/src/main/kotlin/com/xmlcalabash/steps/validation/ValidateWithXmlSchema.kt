package com.xmlcalabash.steps.validation

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.namespace.NsXsi
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonErrorReporter
import net.sf.saxon.Configuration
import net.sf.saxon.Controller
import net.sf.saxon.lib.SchemaURIResolver
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.type.ValidationException
import java.net.URI
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource

open class ValidateWithXmlSchema(): AbstractAtomicStep() {
    companion object {
        private val _useLocationHints = QName("use-location-hints")
        private val _tryNamespaces = QName("try-namespaces")
        private val _targetNamespace = QName("targetNamespace")
    }

    lateinit var document: XProcDocument
    lateinit var errorReporter: SaxonErrorReporter
    val schemas = mutableListOf<XProcDocument>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        // FIXME: I expect this could be more centrally handled...
        errorReporter = SaxonErrorReporter(stepConfig)
        stepConfig.saxonConfig.configuration.setErrorReporterFactory { config -> errorReporter }
    }

    override fun run() {
        super.run()

        document = queues["source"]!!.first()
        schemas.addAll(queues["schema"]!!)

        val manager = stepConfig.processor.getSchemaManager()
        if (manager == null) {
            throw RuntimeException("Schema manager not found, XSD validation requires Saxon EE")
        }

        manager.errorReporter = errorReporter
        manager.schemaURIResolver = XsdResolver()

        validateWithSaxon(manager)
    }

    override fun reset() {
        super.reset()
        document = XProcDocument.ofEmpty(stepConfig)
        schemas.clear()
    }

    private fun validateWithSaxon(manager: SchemaManager) {
        val useLocationHints = booleanBinding(_useLocationHints) ?: false
        val tryNamespaces = booleanBinding(_tryNamespaces) ?: false
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val parameters = qnameMapBinding(Ns.parameters)
        val mode = stringBinding(Ns.mode) ?: "strict"
        val version = stringBinding(Ns.version) ?: "1.1"
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val report = Errors(stepConfig, document.baseURI)
        report.report.metadata.validator("Saxon ${stepConfig.processor.saxonEdition}",
            stepConfig.processor.saxonProductVersion)

        if (version != "1.0" && version != "1.1") {
            throw stepConfig.exception(XProcError.xcXmlSchemaVersionNotAvailable(version))
        }

        val schemaDocuments = mutableListOf<XdmNode>()
        for (schema in schemas) {
            val schemaNode = S9Api.documentElement(schema.value as XdmNode)
            val targetNS = schemaNode.getAttributeValue(_targetNamespace) ?: ""
            stepConfig.debug { "Caching input schema ${schema.baseURI} for ${targetNS}"}

            if (stepConfig.baseUri != null && schema.baseURI != null && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())) {
                // It looks like this one was inline...
                report.report.metadata.schema(schema.baseURI, NsXs.namespace, version, schemaNode)
            } else {
                report.report.metadata.schema(schema.baseURI, NsXs.namespace, version)
            }

            schemaDocuments.add(schema.value as XdmNode)
        }

        val sourceNode = S9Api.documentElement(document.value as XdmNode)
        if (useLocationHints) {
            val nonsSchemaHint = sourceNode.getAttributeValue(NsXsi.noNamespaceSchemaLocation)
            val schemaHint = sourceNode.getAttributeValue(NsXsi.schemaLocation)
            if (nonsSchemaHint != null) {
                val uri = sourceNode.baseURI.resolve(nonsSchemaHint)
                val docManager = stepConfig.environment.documentManager
                val doc = docManager.load(uri, stepConfig)
                schemaDocuments.add(doc.value as XdmNode)
            }
            if (schemaHint != null) {
                val parts = schemaHint.split("\\s+".toRegex())
                var idx = 1
                while (idx < parts.size) {
                    val uri = sourceNode.baseURI.resolve(parts[idx])
                    val docManager = stepConfig.environment.documentManager
                    try {
                        val schema = docManager.load(uri, stepConfig)
                        schemaDocuments.add(schema.value as XdmNode)
                    } catch (ex: Exception) {
                        throw stepConfig.exception(XProcError.xcNotSchemaValidXmlSchema(ex.message ?: "No message given"), ex)
                    }
                    idx += 2
                }
            }
        }

        if (tryNamespaces) {
            val ns = sourceNode.nodeName.namespaceUri
            if (ns != NamespaceUri.NULL) {
                val docManager = stepConfig.environment.documentManager
                try {
                    val doc = docManager.load(URI(ns.toString()), stepConfig)
                    schemaDocuments.add(doc.value as XdmNode)
                } catch (ex: Exception) {
                    stepConfig.debug { "Failed to load XML Shema from ${ns}: ${ex.message}" }
                }
            }
        }

        val saxonConfig = stepConfig.processor.underlyingConfiguration
        saxonConfig.clearSchemaCache()
        for (schema in schemaDocuments) {
            val source = S9Api.xdmToInputSource(stepConfig, schema)
            if (schema.baseURI != null) {
                source.systemId = schema.baseURI.toString()
            }
            try {
                manager.load(SAXSource(source))
            } catch (ex: SaxonApiException) {
                if (schema.baseURI != null) {
                    throw stepConfig.exception(XProcError.xcXmlSchemaInvalidSchema(schema.baseURI!!), ex)
                }
                throw stepConfig.exception(XProcError.xcXmlSchemaInvalidSchema(), ex)
            }
        }

        val destination = XdmDestination()
        val controller = Controller(saxonConfig)
        val pipe = controller.makePipelineConfiguration()
        val preceiver = destination.getReceiver(pipe, SerializationProperties())
        pipe.setRecoverFromValidationErrors(assertValid)
        pipe.errorReporter = errorReporter
        preceiver.setPipelineConfiguration(pipe)

        val validator = manager.newSchemaValidator()
        val invalidityHandler = validator.invalidityHandler

        val listener = CachingErrorListener(stepConfig, report, invalidityHandler)

        validator.destination = destination
        validator.invalidityHandler = listener
        validator.isLax = mode == "lax"
        validator.isUseXsiSchemaLocation = useLocationHints

        var raisedException: XProcError? = null

        try {
            validator.validate((document.value as XdmNode).asSource())
        } catch (ex: SaxonApiException) {
            val errors = report.asXml()
            var msg = ex.message
            if (listener.exceptions.isNotEmpty()) {
                val lex = listener.exceptions.first()
                when (lex) {
                    is ValidationException -> {
                        msg = lex.message
                        val fail = lex.validationFailure
                        val except = if (document.baseURI == null) {
                            XProcError.xcNotSchemaValidXmlSchema(msg!!)
                        } else {
                            XProcError.xcNotSchemaValidXmlSchema(document.baseURI!!, msg!!)
                        }
                        raisedException = except
                    }
                }
            } else {
                val except = if (document.baseURI == null) {
                    XProcError.xcNotSchemaValidXmlSchema(ex.message!!)
                } else {
                    XProcError.xcNotSchemaValidXmlSchema(document.baseURI!!, ex.message!!)
                }
                raisedException = except
            }
        } catch (ex: Exception) {
            val except = if (document.baseURI == null) {
                XProcError.xcNotSchemaValidXmlSchema(ex.message!!)
            } else {
                XProcError.xcNotSchemaValidXmlSchema(document.baseURI!!, ex.message!!)
            }
            raisedException = except
        }

        val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)

        if (raisedException != null) {
            if (assertValid) {
                if (raisedException.code == NsErr.xc(156)) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidXmlSchema(xvrl))
                }
                throw raisedException.exception()
            } else {
                receiver.output("result", document)
                receiver.output("report", xvrl)
            }
        } else {
            receiver.output("result", XProcDocument.ofXml(destination.xdmNode, stepConfig))
            receiver.output("report", xvrl)
        }
    }

    override fun toString(): String = "p:validate-with-xml-schema"

    inner class XsdResolver(): SchemaURIResolver {
        override fun setConfiguration(config: Configuration?) {
            // I don't care???
        }

        override fun resolve(moduleURI: String?, baseURI: String?, locations: Array<out String>?): Array<StreamSource>? {
            return stepConfig.environment.documentManager.resolve(moduleURI, baseURI, locations)
        }
    }
}