package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

class WhenInstruction(parent: ChooseInstruction, name: String?): CompoundStepDeclaration(parent, parent.stepConfig.newInstance(), NsP.`when`, name) {
    companion object {
        private val INVALID_TEST = "*** no test specified ***"
    }

    override val contentModel = anySteps + mapOf(NsP.withInput to '1', NsP.output to '*')

    private val expressionContext = mutableListOf<ConnectionInstruction>()
    lateinit var testExpression: XProcSelectExpression
    var test: String = INVALID_TEST
        set(value) {
            checkOpen()
            field = value
        }

    var collection: Boolean? = null
        set(value) {
            checkOpen()
            field = value
        }

    private val variables = mutableSetOf<QName>()

    override fun elaborate() {
        val withInput = children.filterIsInstance<WithInputInstruction>().firstOrNull()
        if (withInput != null) {
            withInput.port = "!source"
        }

        super.elaborate()

        if (test == INVALID_TEST) {
            reportError(XProcError.xsMissingRequiredAttribute(Ns.test))
        }
        testExpression = XProcExpression.select(stepConfig, test, stepConfig.parseXsSequenceType("xs:boolean"))
    }

    override fun staticAnalysis(context: InstructionStaticContext) {
        this.context = context.copy()

        val withInput = children.filterIsInstance<WithInputInstruction>().firstOrNull()
        if (withInput != null) {
            for (child in withInput.children) {
                child.staticAnalysis(context)
                expressionContext.add(child as ConnectionInstruction)
            }
            _children.remove(withInput)
        }

        if (testExpression.contextRef && context.drp == null) {
            reportError(XProcError.xsPortNotReadable())
        }

        for ((name, value) in context.inscopeVariables) {
            if (value.staticValue != null) {
                testExpression.setStaticBinding(name, value.staticValue!!)
            }

            if (testExpression.canBeResolvedStatically()) {
                testExpression.computeStaticValue(context)
            }

            // FIXME: check static type?
        }

        variables.addAll(testExpression.variableRefs)

        super.staticAnalysis(context)
    }

    override fun rewrite() {
        val newChildren = mutableListOf<XProcInstruction>()
        val currentSteps = mutableListOf<XProcInstruction>()
        for (child in children) {
            if (currentSteps.isNotEmpty() || child is StepDeclaration) {
                currentSteps.add(child)
            } else {
                newChildren.add(child)
            }
        }

        if (testExpression.staticValue == null) {
            val newSteps = mutableListOf<XProcInstruction>()
            val readFrom = mutableListOf<PortBindingContainer>()
            if (testExpression.contextRef) {
                for (conn in expressionContext) {
                    val promoted = conn.promoteToStep(this)
                    val last = promoted.last()
                    context.addInscopeStepName(last)
                    readFrom.add(last.primaryOutput()!!)
                    newSteps.addAll(promoted)
                }
            }

            newSteps.addAll(testExpression.promoteToStep(this, context, readFrom))
            newChildren.addAll(newSteps)
        } else {
            val expr = XProcExpression.constant(stepConfig, testExpression.staticValue!!, stepConfig.parseXsSequenceType("xs:boolean"))
            val exprStep = AtomicExpressionStepInstruction(this, expr)

            exprStep.elaborate()
            exprStep.staticAnalysis(context)
            newChildren.add(exprStep)
        }

        val last = newChildren.last()
        context.addInscopeStepName(last as StepDeclaration)

        /*
        var contextwi = children.filterIsInstance<WithInputInstruction>()
        if (contextwi.isEmpty()) {
            val choose = parent as ChooseInstruction
            contextwi = choose.children.filterIsInstance<WithInputInstruction>()
        }
        
         */

        val guard = GuardExpressionStepInstruction(this)

        val wi = guard.withInput()
        val pipe = wi.pipe()
        pipe.setReadablePort(last.primaryOutput()!!)

/*
        if (contextwi.isNotEmpty()) {
            val wi = guard.withInput()
            for (cpipe in contextwi.first().children) {
                val pipe = wi.pipe()
                pipe.setReadablePort((cpipe as PipeInstruction).readablePort!!)
            }
        }
 */

        guard.elaborate()
        guard.staticAnalysis(context)

        newChildren.add(guard)
        newChildren.addAll(currentSteps)

        _children.clear()
        _children.addAll(newChildren)

        super.rewrite()
    }
}