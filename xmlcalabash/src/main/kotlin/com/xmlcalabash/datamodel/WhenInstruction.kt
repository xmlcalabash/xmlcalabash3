package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class WhenInstruction(parent: ChooseInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.`when`) {
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
    private var guardDrp: PortBindingContainer? = null
    private var explicitBinding = false

    // When dealing with p:if, we end up elaborating the p:when "early". Don't do it twice.
    private var elaborated = false
    override fun elaborateInstructions() {
        if (elaborated) {
            return
        }
        elaborated = true

        if (depends.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsAttributeForbidden(Ns.depends))
        }

        if (test == INVALID_TEST) {
            throw stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.test))
        }

        testExpression = XProcExpression.select(stepConfig, test, stepConfig.parseXsSequenceType("xs:boolean?"), collection == true)

        guardDrp = (parent as ChooseInstruction).namedInput("!context")
        if (guardDrp == null) {
            guardDrp = stepConfig.drp
        }

        val withInput = children.filterIsInstance<WithInputInstruction>().firstOrNull()
        if (withInput != null) {
            explicitBinding = true

            if (withInput.portDefined) {
                throw stepConfig.exception(XProcError.xsPortNameNotAllowed())
            }

            withInput._port = "!source"
            withInput.elaborateInstructions()
            for (child in withInput.children) {
                expressionContext.add(child as ConnectionInstruction)
            }
            _children.remove(withInput)
            guardDrp = withInput
        }

        if (testExpression.contextRef && guardDrp == null) {
            if ((parent as ChooseInstruction).namedInput("!context") == null) {
                stepConfig.warn { "Expression refers to context when no context is available" }
            }
        }

        for ((name, value) in stepConfig.inscopeVariables) {
            if (value.canBeResolvedStatically()) {
                testExpression.setStaticBinding(name, value.select!!)
            }

            if (testExpression.canBeResolvedStatically()) {
                testExpression.computeStaticValue(stepConfig)
            }

            // FIXME: check static type?
        }

        variables.addAll(testExpression.variableRefs)

        super.elaborateInstructions()
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
            if (testExpression.contextRef || guardDrp != null) {
                if (expressionContext.isNotEmpty()) {
                    for (conn in expressionContext) {
                        if (conn is PipeInstruction) {
                            readFrom.add(conn.readablePort!!)
                        } else {
                            val promoted = conn.promoteToStep(this, this)
                            val last = promoted.last()
                            stepConfig.addVisibleStepName(last)
                            readFrom.add(last.primaryOutput()!!)
                            newSteps.addAll(promoted)
                        }
                    }
                } else {
                    if (guardDrp != null) {
                        readFrom.add(guardDrp!!)
                    }
                }
            }

            newSteps.addAll(testExpression.promoteToStep(this, readFrom, explicitBinding))
            newChildren.addAll(newSteps)
        } else {
            val expr = XProcExpression.constant(stepConfig, testExpression.staticValue!!, stepConfig.parseXsSequenceType("xs:boolean"))
            val exprStep = AtomicExpressionStepInstruction(this, expr)

            exprStep.elaborateAtomicStep()
            newChildren.add(exprStep)
        }

        val last = newChildren.last()
        stepConfig.addVisibleStepName(last as StepDeclaration)

        val guard = GuardExpressionStepInstruction(this)
        val wi = guard.withInput()
        val pipe = wi.pipe()
        pipe.setReadablePort(last.primaryOutput()!!)

        guard.elaborateAtomicStep()

        newChildren.add(guard)
        newChildren.addAll(currentSteps)

        _children.clear()
        _children.addAll(newChildren)

        super.rewrite()
    }
}