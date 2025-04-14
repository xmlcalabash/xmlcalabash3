package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType

abstract class VariableBindingContainer(parent: XProcInstruction, val name: QName, stepConfig: InstructionConfiguration, instructionType: QName): BindingContainer(parent, stepConfig, instructionType) {

    internal var _asType: SequenceType? = null
    var asType: SequenceType?
        get() = _asType
        set(value) {
            checkOpen()
            _asType = value
        }

    internal var _specialType: SpecialType? = null
    var specialType: SpecialType?
        get() = _specialType
        set(value) {
            checkOpen()
            _specialType = value
        }

    internal var _select: XProcExpression? = null
    open var select: XProcExpression?
        get() = _select
        set(value) {
            checkOpen()
            _select = value
        }

    internal var _collection: Boolean? = null
    var collection: Boolean?
        get() = _collection
        set(value) {
            checkOpen()
            _collection = value
        }

    internal var _href: XProcExpression? = null
    var href: XProcExpression?
        get() = _href
        set(value) {
            checkOpen()
            if (value == null) {
                _href = null
            } else {
                _href = value.cast(stepConfig.typeUtils.parseXsSequenceType("xs:anyURI"))
            }
        }

    var pipe: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal var exprStep: AtomicExpressionStepInstruction? = null

    internal var withInput: WithInputInstruction? = null
    protected var alwaysDynamic = false

    internal var _withOutput: WithOutputInstruction? = null
    val withOutput: WithOutputInstruction?
        get() = _withOutput

    open fun canBeResolvedStatically(): Boolean {
        if (alwaysDynamic) {
            return false
        }

        val exprSelect = select
        if (exprSelect == null) {
            return false
        }

        // Selection patterns don't have a context reference...
        return exprSelect.canBeResolvedStatically(specialType != SpecialType.XSLT_SELECTION_PATTERN)
    }

    override fun elaborateInstructions() {
        asType = asType ?: stepConfig.typeUtils.parseSequenceType("item()*")

        if (href != null && children.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsHrefAndChildren())
        }

        if (pipe != null && children.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsPipeAndChildren())
        }

        if (href != null && pipe != null) {
            throw stepConfig.exception(XProcError.xsHrefAndPipe())
        }

        href?.let { promoteHref(it) }
        href = null

        pipe?.let { promotePipe(it) }
        pipe = null

        if (select!!.contextRef && children.isEmpty() && stepConfig.drp != null) {
            if (specialType != SpecialType.XSLT_SELECTION_PATTERN) {
                val readFrom = pipe()
                readFrom.setReadablePort(stepConfig.drp!!, true)
            }
        }

        super.elaborateInstructions()

        if (name in stepConfig.inscopeVariables) {
            val inScope = stepConfig.inscopeVariables[name]!!
            if (inScope is OptionInstruction && inScope.static) {
                if (this is OptionInstruction) {
                    throw stepConfig.exception(XProcError.xsShadowStaticOption(name))
                } else {
                    throw stepConfig.exception(XProcError.xsVariableShadowsStaticOption(name))
                }
            }
        }

        // If this is a static option with a value provided at compile time,
        // staticValue will already have a value. Otherwise, we might
        // calculate it.
        if (canBeResolvedStatically()) {
            for ((name, value) in stepConfig.inscopeVariables) {
                if (value.canBeResolvedStatically()) {
                    val details = builder.staticOptionsManager.get(value)
                    select!!.setStaticBinding(name, details.staticValue)
                }
            }

            val eager = stepConfig.eagerEvaluation
            if (eager) {
                // Make sure it doesn't throw an exception
                select!!.computeStaticValue(stepConfig)
            }
        }
    }

    open fun promoteToStep(step: XProcInstruction): List<AtomicStepInstruction> {
        if (!alwaysDynamic && canBeResolvedStatically()) {
            return emptyList()
        }

        exprStep = AtomicExpressionStepInstruction(step, name, select!!)
        step.stepConfig.addVisibleStepName(exprStep!!)

        if (this is OptionInstruction) {
            exprStep!!.externalName = name
        }

        for (name in select!!.variableRefs) {
            if (name !in stepConfig.inscopeVariables) {
                throw stepConfig.exception(XProcError.xsXPathStaticError("Expression refers to \$${name} which is not in scope."))
            }
            val variable = stepConfig.inscopeVariables[name]!!
            if (variable.withOutput != null) {
                val wi = exprStep!!.withInput()
                wi._port = "Q{${name.namespaceUri}}${name.localName}"
                val pipe = wi.pipe()
                pipe.setReadablePort(variable.withOutput!!, false)
            }
        }

        val steps = mutableListOf<AtomicStepInstruction>()
        if (children.isNotEmpty()) {
            val wi = exprStep!!.withInput()
            wi._sequence = true
            wi._port = "source"
            for (child in children) {
                when (child) {
                    is PipeInstruction -> {
                        val pipe = wi.pipe()
                        pipe.setReadablePort(child.readablePort!!, false)
                    }
                    else -> {
                        val csteps = (child as ConnectionInstruction).promoteToStep(step as StepDeclaration, step)
                        if (csteps.isNotEmpty()) {
                            val last = csteps.last()
                            val pipe = wi.pipe()
                            pipe.setReadablePort(last.primaryOutput()!!, false)
                        }
                        steps.addAll(csteps)
                    }
                }
            }
        }

        val wo = exprStep!!.withOutput()
        wo.port = "result"
        wo.primary = true
        wo.sequence = false

        steps.add(exprStep!!)
        return steps
    }

    fun primaryOutput(): WithOutputInstruction {
        for (child in children) {
            if (child is WithOutputInstruction) {
                return child
            }
        }
        throw stepConfig.exception(XProcError.xiImpossible("expression has no output"))
    }

    override fun toString(): String {
        return "${instructionType}/${id} ${name}"
    }
}