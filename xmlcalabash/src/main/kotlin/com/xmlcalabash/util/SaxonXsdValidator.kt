package com.xmlcalabash.util

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.namespace.NsXsi
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.steps.validation.CachingErrorListener
import com.xmlcalabash.steps.validation.Errors
import net.sf.saxon.Controller
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.type.ValidationException
import java.net.URI
import javax.xml.transform.sax.SAXSource

class SaxonXsdValidator(val stepConfig: XProcStepConfiguration) {
    var useLocationHints = stepConfig.xmlCalabashConfig.useLocationHints
    var tryNamespaces = stepConfig.xmlCalabashConfig.tryNamespaces
    var mode = stepConfig.validationMode
    var assertValid = true
    var parameters = mapOf<QName,XdmValue>()
    var version = "1.1"
    var reportFormat = "xvrl"
    val schemas = mutableListOf<XProcDocument>()
    val errorReporter = SaxonErrorReporter(stepConfig)
    var xvrl: XProcDocument? = null

    fun validate(source: XProcDocument): XProcDocument {
        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        if (version != "1.0" && version != "1.1") {
            throw stepConfig.exception(XProcError.xcXmlSchemaVersionNotAvailable(version))
        }

        val manager = stepConfig.processor.getSchemaManager()
        if (manager == null) {
            throw RuntimeException("Schema manager not found, XSD validation requires Saxon EE")
        }

        manager.errorReporter = errorReporter
        manager.schemaURIResolver = XsdResolver(stepConfig)

        val report = Errors(stepConfig, source.baseURI)
        report.report.metadata.validator(
            "Saxon ${stepConfig.processor.saxonEdition}",
            stepConfig.processor.saxonProductVersion
        )

        val schemaDocuments = mutableListOf<XdmNode>()
        for (schema in schemas) {
            val schemaNode = S9Api.documentElement(schema.value as XdmNode)
            val targetNS = schemaNode.getAttributeValue(Ns.targetNamespace) ?: ""
            stepConfig.debug { "Caching input schema ${schema.baseURI} for ${targetNS}" }
            report.report.metadata.schema(schema.baseURI, NsXs.namespace, version)
            schemaDocuments.add(schema.value as XdmNode)
        }

        val sourceNode = S9Api.documentElement(source.value as XdmNode)
        if (useLocationHints) {
            val nonsSchemaHint = sourceNode.getAttributeValue(NsXsi.noNamespaceSchemaLocation)
            val schemaHint = sourceNode.getAttributeValue(NsXsi.schemaLocation)
            if (nonsSchemaHint != null) {
                val uri =  UriUtils.resolve(sourceNode.baseURI, nonsSchemaHint)!!
                val docManager = stepConfig.environment.documentManager
                val doc = docManager.load(uri, stepConfig)
                schemaDocuments.add(doc.value as XdmNode)
            }
            if (schemaHint != null) {
                val parts = schemaHint.split("\\s+".toRegex())
                var idx = 1
                while (idx < parts.size) {
                    val uri = UriUtils.resolve(sourceNode.baseURI, parts[idx])!!
                    val docManager = stepConfig.environment.documentManager
                    try {
                        val schema = docManager.load(uri, stepConfig)
                        schemaDocuments.add(schema.value as XdmNode)
                    } catch (ex: Exception) {
                        throw stepConfig.exception(
                            XProcError.xcNotSchemaValidXmlSchema(
                                ex.message ?: "No message given"
                            ), ex
                        )
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

        stepConfig.saxonConfig.clearSchemaCache();
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
        validator.isLax = mode == ValidationMode.LAX
        validator.isUseXsiSchemaLocation = useLocationHints

        var raisedException: XProcError? = null

        try {
            validator.validate((source.value as XdmNode).asSource())
        } catch (ex: SaxonApiException) {
            var msg = ex.message
            if (listener.exceptions.isNotEmpty()) {
                val lex = listener.exceptions.first()
                when (lex) {
                    is ValidationException -> {
                        msg = lex.message
                        val except = if (source.baseURI == null) {
                            XProcError.xcNotSchemaValidXmlSchema(msg!!)
                        } else {
                            XProcError.xcNotSchemaValidXmlSchema(source.baseURI!!, msg!!)
                        }
                        raisedException = except
                    }
                }
            } else {
                val except = if (source.baseURI == null) {
                    XProcError.xcNotSchemaValidXmlSchema(ex.message!!)
                } else {
                    XProcError.xcNotSchemaValidXmlSchema(source.baseURI!!, ex.message!!)
                }
                raisedException = except
            }
        } catch (ex: Exception) {
            val except = if (source.baseURI == null) {
                XProcError.xcNotSchemaValidXmlSchema(ex.message!!)
            } else {
                XProcError.xcNotSchemaValidXmlSchema(source.baseURI!!, ex.message!!)
            }
            raisedException = except
        }

        xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)

        if (raisedException != null) {
            if (assertValid) {
                if (raisedException.code == NsErr.xc(156)) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidXmlSchema(xvrl!!))
                }
                throw raisedException.exception()
            } else {
                return source
            }
        } else {
            val vmode = if (mode == ValidationMode.STRICT) {
                "strict"
            } else {
                "lax"
            }

            val props = DocumentProperties(source.properties)
            props[NsCx.validationMode] = XdmAtomicValue(vmode)

            // Special case, if the input document has no base URI, don't let this
            // validation manufacture a default one...
            if (props.baseURI == null) {
                props[Ns.baseUri] = XdmEmptySequence.getInstance()
            }

            return XProcDocument.ofXml(destination.xdmNode, stepConfig, props)
        }
    }
}