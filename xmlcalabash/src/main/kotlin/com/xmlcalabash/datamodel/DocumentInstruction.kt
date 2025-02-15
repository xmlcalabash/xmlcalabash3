package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.*

class DocumentInstruction private constructor(parent: XProcInstruction): ConnectionInstruction(parent, NsP.document) {
    constructor(parent: XProcInstruction, href: XProcExpression): this(parent) {
        this.href = href.cast(parent.stepConfig.parseXsSequenceType("xs:anyURI"))
    }

    var href: XProcExpression = XProcExpression.error(parent.stepConfig)
        set(value) {
            checkOpen()
            field = value.cast(parent!!.stepConfig.parseXsSequenceType("xs:anyURI"))
        }

    var contentType: MediaType? = null
        set(value) {
            checkOpen()
            field = value
        }

    private var _documentProperties: XProcExpression? = null
    var documentProperties: XProcExpression
        get() = _documentProperties!!
        set(value) {
            checkOpen()
            _documentProperties = value.cast(parent!!.stepConfig.qnameMapType)
        }

    private var _parameters: XProcExpression? = null
    var parameters: XProcExpression
        get() = _parameters!!
        set(value) {
            checkOpen()
            _parameters = value.cast(parent!!.stepConfig.qnameMapType)
        }

    private val variables = mutableSetOf<QName>()

    override fun elaborateInstructions() {
        if (_documentProperties == null) {
            _documentProperties = XProcExpression.constant(parent!!.stepConfig, XdmMap(), parent!!.stepConfig.qnameMapType)
        }

        if (_parameters == null) {
            _parameters = XProcExpression.constant(parent!!.stepConfig, XdmMap(), parent!!.stepConfig.qnameMapType)
        }

        for (child in children) {
            throw stepConfig.exception(XProcError.xsInvalidElement(child.instructionType))
        }

        if ((parameters.contextRef || documentProperties.contextRef) && stepConfig.drp == null) {
            throw stepConfig.exception(XProcError.xsNoPortPortNotReadable())
        }

        variables.addAll(href.variableRefs)
        variables.addAll(documentProperties.variableRefs)
        variables.addAll(parameters.variableRefs)

        super.elaborateInstructions()
    }

    override fun promoteToStep(parent: StepDeclaration, step: StepDeclaration): List<AtomicStepInstruction> {
        val newSteps = mutableListOf<AtomicStepInstruction>()

        val docStep = AtomicDocumentStepInstruction(parent)
        docStep.depends.addAll(step.depends)

        if (href.canBeResolvedStatically()) {
            docStep._staticOptions[Ns.href] = StaticOptionDetails(href.stepConfig, Ns.href, stepConfig.parseXsSequenceType("xs:anyURI"), emptyList(), href)
        } else {
            val bindings = if (stepConfig.drp == null) {
                emptyList()
            } else {
                listOf(stepConfig.drp!!)
            }
            val exprSteps = href.promoteToStep(parent, bindings, false)
            val expr = exprSteps.last()

            val wi = docStep.withInput()
            wi._port = "Q{}href"
            val pipe = wi.pipe()
            pipe.setReadablePort(expr.primaryOutput()!!)
            newSteps.addAll(exprSteps)
        }

        if (documentProperties.canBeResolvedStatically()) {
            docStep._staticOptions[Ns.documentProperties] = StaticOptionDetails(stepConfig, Ns.documentProperties, stepConfig.qnameMapType, emptyList(), documentProperties)
        } else {
            val exprSteps = documentProperties.promoteToStep(parent, listOf(stepConfig.drp!!), false)
            val expr = exprSteps.last()

            val wi = docStep.withInput()
            wi._port = "Q{}document-properties"
            val pipe = wi.pipe()
            pipe.setReadablePort(expr.primaryOutput()!!)
            newSteps.addAll(exprSteps)
        }

        if (parameters.canBeResolvedStatically()) {
            docStep._staticOptions[Ns.parameters] = StaticOptionDetails(stepConfig, Ns.parameters, stepConfig.qnameMapType, emptyList(), parameters)
        } else {
            val exprSteps = parameters.promoteToStep(parent, listOf(stepConfig.drp!!), false)
            val expr = exprSteps.last()

            val wi = docStep.withInput()
            wi._port = "Q{}parameters"
            val pipe = wi.pipe()
            pipe.setReadablePort(expr.primaryOutput()!!)
            newSteps.addAll(exprSteps)
        }

        docStep.documentProperties = documentProperties
        docStep.parameters = parameters
        docStep.contentType = contentType

        // docStep.depends.addAll(findStep().depends)

        docStep.elaborateAtomicStep()

        // docStep.stepDepends(staticContext)

        val output = docStep.primaryOutput()!!
        output._sequence = false
        output._contentTypes.clear()

        // Don't limit what p:document can output; the content-type is about what it's
        // reading. That's usually, but not necessarily, the same as what it outputs.
        // (See the RdfLoader)
        output._contentTypes.add(MediaType.ANY)

        newSteps.add(docStep)
        return newSteps
    }
}