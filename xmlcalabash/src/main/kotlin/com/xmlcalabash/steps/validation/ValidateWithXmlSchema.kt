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
import com.xmlcalabash.util.SaxonXsdValidator
import net.sf.saxon.Controller
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.type.ValidationException
import java.net.URI
import javax.xml.transform.sax.SAXSource

open class ValidateWithXmlSchema(): AbstractAtomicStep() {
    lateinit var document: XProcDocument
    lateinit var errorReporter: SaxonErrorReporter

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        // FIXME: I expect this could be more centrally handled...
        errorReporter = SaxonErrorReporter(stepConfig)
        stepConfig.saxonConfig.configuration.setErrorReporterFactory { config -> errorReporter }
    }

    override fun run() {
        super.run()
        document = queues["source"]!!.first()
        validateWithSaxon()
    }

    override fun reset() {
        super.reset()
        document = XProcDocument.ofEmpty(stepConfig)
    }

    private fun validateWithSaxon() {
        val validator = SaxonXsdValidator(stepConfig)
        validator.useLocationHints = booleanBinding(Ns.useLocationHints) ?: false
        validator.tryNamespaces = booleanBinding(Ns.tryNamespaces) ?: false
        validator.assertValid = booleanBinding(Ns.assertValid) ?: true
        validator.parameters = qnameMapBinding(Ns.parameters)
        validator.version = stringBinding(Ns.version) ?: "1.1"
        validator.reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"
        if (stringBinding(Ns.mode) == "strict") {
            validator.mode = ValidationMode.STRICT
        } else {
            validator.mode = ValidationMode.LAX
        }
        validator.schemas.clear()
        validator.schemas.addAll(queues["schema"]!!)

        val validated = validator.validate(document)
        receiver.output("result", validated)
        receiver.output("report", validator.xvrl!!)
    }

    override fun toString(): String = "p:validate-with-xml-schema"
}