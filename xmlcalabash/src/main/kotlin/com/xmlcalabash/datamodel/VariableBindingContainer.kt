package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmValue

abstract class VariableBindingContainer(
    parent: XProcInstruction,
    val name: QName,
    stepConfig: InstructionConfiguration,
    instructionType: QName
): BindingContainer(parent, stepConfig, instructionType) {

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
                _href = value.cast(stepConfig.parseXsSequenceType("xs:anyURI"))
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
        return !alwaysDynamic && select?.canBeResolvedStatically() ?: false
    }

    override fun elaborateInstructions() {
        asType = asType ?: stepConfig.parseSequenceType("item()*")

        if (href != null && children.isNotEmpty()) {
            throw XProcError.xsHrefAndChildren().exception()
        }

        if (pipe != null && children.isNotEmpty()) {
            throw XProcError.xsPipeAndChildren().exception()
        }

        if (href != null && pipe != null) {
            throw XProcError.xsHrefAndPipe().exception()
        }

        href?.let { promoteHref(it) }
        href = null

        pipe?.let { promotePipe(it) }
        pipe = null

        if (select!!.contextRef && children.isEmpty()
            && stepConfig.drp != null) {
            val readFrom = pipe()
            readFrom.setReadablePort(stepConfig.drp!!)
        }

        super.elaborateInstructions()

        if (name in stepConfig.inscopeVariables) {
            val inScope = stepConfig.inscopeVariables[name]!!
            if (inScope is OptionInstruction && inScope.static) {
                if (this is OptionInstruction) {
                    throw XProcError.xsShadowStaticOption(name).exception()
                } else {
                    throw XProcError.xsVariableShadowsStaticOption(name).exception()
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

            val eager = stepConfig.environment.commonEnvironment.eagerEvaluation
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

        exprStep = AtomicExpressionStepInstruction(step, select!!)
        step.stepConfig.addVisibleStepName(exprStep!!)

        if (this is OptionInstruction) {
            exprStep!!.externalName = name
        }

        for (name in select!!.variableRefs) {
            val variable = stepConfig.inscopeVariables[name]!!
            if (variable.withOutput != null) {
                val wi = exprStep!!.withInput()
                wi._port = "Q{${name.namespaceUri}}${name.localName}"
                val pipe = wi.pipe()
                pipe.setReadablePort(variable.withOutput!!)
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
                        pipe.setReadablePort(child.readablePort!!)
                    }
                    else -> {
                        val csteps = (child as ConnectionInstruction).promoteToStep(step as StepDeclaration, step)
                        if (csteps.isNotEmpty()) {
                            val last = csteps.last()
                            val pipe = wi.pipe()
                            pipe.setReadablePort(last.primaryOutput()!!)
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
    throw XProcError.xiImpossible("expression has no output").exception()
}

    override fun toString(): String {
        return "${instructionType}/${id} ${name}"
    }
}