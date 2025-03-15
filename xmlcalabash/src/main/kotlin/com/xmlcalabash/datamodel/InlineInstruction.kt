package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.ValueTemplateFilter
import com.xmlcalabash.util.ValueTemplateFilterNone
import com.xmlcalabash.util.ValueTemplateFilterXml
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode

class InlineInstruction(parent: XProcInstruction, xmlDocument: XdmNode): ConnectionInstruction(parent, NsP.inline) {
    private var _xml: XdmNode = xmlDocument
    var xml: XdmNode
        get() = _xml
        set(value) {
            checkOpen()
            _xml = value
        }

    private var _contentType: MediaType? = null
    var contentType: MediaType?
        get() = _contentType
        set(value) {
            checkOpen()
            _contentType = value
        }

    private var _documentProperties: XProcExpression? = null
    var documentProperties: XProcExpression
        get() = _documentProperties!!
        set(value) {
            checkOpen()
            _documentProperties = value.cast(parent!!.stepConfig.qnameMapType)
        }

    var encoding: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    private val variables = mutableSetOf<QName>()
    private lateinit var _valueTemplateFilter: ValueTemplateFilter
    val valueTemplateFilter: ValueTemplateFilter
        get() = _valueTemplateFilter

    override fun elaborateInstructions() {
        if (_documentProperties == null) {
            val stype = "map(Q{${NsXs.namespace}}QName, item()*)"
            _documentProperties = XProcExpression.constant(parent!!.stepConfig, XdmMap(), parent!!.stepConfig.parseSequenceType(stype))
        }

        val isRunPipeline = false // = parent != null && parent!!.parent is RunBuilder

        // Force inlines to have unique URIs because document-uri(). Bleh.
        val uri = xml.baseURI?.toString() ?: ""
        val inlineBaseUri = stepConfig.environment.uniqueUri(uri)

        if (contentType == null) {
            _contentType = MediaType.XML
        }
        val markupContentType = contentType!!.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)

        if (encoding != null) {
            if (encoding != "base64") {
                throw stepConfig.exception(XProcError.xsUnsupportedEncoding(encoding!!))
            }
            if (markupContentType) {
                throw stepConfig.exception(XProcError.xdEncodingWithXmlOrHtml(encoding!!))
            }
        }

        _valueTemplateFilter = if (encoding == null && !isRunPipeline) {
            ValueTemplateFilterXml(xml, contentType!!, inlineBaseUri)
        } else {
            ValueTemplateFilterNone(xml, inlineBaseUri)
        }

        val staticBindings = mutableMapOf<QName, XProcExpression>()
        for ((name, value) in stepConfig.inscopeVariables) {
            if (value.canBeResolvedStatically()) {
                val details = builder.staticOptionsManager.get(value)
                staticBindings[name] = details.staticValue
                documentProperties.setStaticBinding(name, details.staticValue)
            }
        }

        _xml = _valueTemplateFilter.expandStaticValueTemplates(stepConfig, expandText!!, staticBindings)

        if (!markupContentType && _valueTemplateFilter.containsMarkup(stepConfig)) {
            stepConfig.warn { "Markup detected in ${contentType} inline" }
        }

        val usesContext = _valueTemplateFilter.usesContext()
        variables.addAll(_valueTemplateFilter.usesVariables())

        if (usesContext) {
            val wi = WithInputInstruction(this, stepConfig)
            wi.port = "source"
            _children.add(wi)
            wi.pipe()
        }

        super.elaborateInstructions()
    }

    override fun promoteToStep(parent: StepDeclaration, step: StepDeclaration): List<AtomicStepInstruction> {
        val newSteps = mutableListOf<AtomicStepInstruction>()

        val inlineStep = AtomicInlineStepInstruction(this, valueTemplateFilter)
        stepConfig.addVisibleStepName(inlineStep)
        inlineStep.depends.addAll(step.depends)

        for (name in variables) {
            val binding = stepConfig.inscopeVariables[name] ?: throw stepConfig.exception(XProcError.xsXPathStaticError(name))
            if (!binding.canBeResolvedStatically()) {
                val eqname = "Q{${name.namespaceUri}}${name.localName}"
                val wi = WithInputInstruction(this, stepConfig)
                wi._port = eqname
                val pipe = wi.pipe()
                val vbuilder = when (binding) {
                    is VariableInstruction -> binding.exprStep!!
                    is OptionInstruction -> binding.exprStep!!
                    else -> throw IllegalStateException("Invalid binding: ${binding}")
                }
                pipe.setReadablePort(vbuilder.primaryOutput()!!, false)
                inlineStep._children.add(wi)
            }
        }

        if (documentProperties.canBeResolvedStatically()) {
            inlineStep._staticOptions[Ns.documentProperties] = StaticOptionDetails(stepConfig.copy(), Ns.documentProperties, stepConfig.qnameMapType, emptyList(), documentProperties)
        } else {
            val context = if (stepConfig.drp == null) {
                emptyList<PortBindingContainer>()
            } else {
                listOf(stepConfig.drp!!)
            }
            val exprSteps = documentProperties.promoteToStep(parent, NsCx.anonymous, context, false)
            val expr = exprSteps.last()

            val wi = inlineStep.withInput()
            wi._port = "Q{}document-properties"
            val pipe = wi.pipe()
            pipe.setReadablePort(expr.primaryOutput()!!, false)
            newSteps.addAll(exprSteps)
        }

        // If there's no DRP, make sure there's an empty binding for the inlineStep
        // (If there's supposed to be a context binding, staticAnalysis() has inserted
        // an empty WithInput to hold the binding to the drp.)
        if (stepConfig.drp == null || children.isEmpty()) {
            //val emptyStep = addRewriteEmpty(parent)
            //newSteps.add(emptyStep)
            //inlineStep.stepConfig.addVisibleStepName(emptyStep)

            val wi = inlineStep.withInput()
            wi.port = "source"
            wi.primary = true
            wi.sequence = true
            wi.weldedShut = true
            //val pipe = wi.pipe()
            //pipe.setReadablePort(emptyStep.primaryOutput()!!)
        } else {
            val bindingChildren = if (children.isNotEmpty()) {
                children.first().children
            } else {
                emptyList()
            }

            if (bindingChildren.isNotEmpty()) {
                val children = children.first().children
                val wi = inlineStep.withInput()
                wi.port = "source"
                for (child in bindingChildren) {
                    when (child) {
                        is PipeInstruction -> {
                            val pipe = wi.pipe()
                            pipe.setReadablePort(child.readablePort!!, false)
                        }
                        else -> {
                            val csteps = (child as ConnectionInstruction).promoteToStep(parent, step)
                            if (csteps.isNotEmpty()) {
                                val last = csteps.last()
                                val pipe = wi.pipe()
                                pipe.setReadablePort(last.primaryOutput()!!, false)
                            }
                            newSteps.addAll(csteps)
                        }
                    }
                }
            }
        }

        inlineStep.elaborateAtomicStep()

        val output = inlineStep.primaryOutput()!!

        // Don't limit what p:inline can output; the content-type is about what it's
        // reading. That's usually, but not necessarily, the same as what it outputs.
        // (See the RdfLoader)
        output._contentTypes.clear()

        newSteps.add(inlineStep)
        return newSteps
    }
}