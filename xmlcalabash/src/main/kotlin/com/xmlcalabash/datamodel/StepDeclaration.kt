package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.AssertionsMonitor
import com.xmlcalabash.util.DurationUtils
import net.sf.saxon.s9api.QName
import java.time.Duration

abstract class StepDeclaration(parent: XProcInstruction?, stepConfig: InstructionConfiguration, instructionType: QName): XProcInstruction(parent, stepConfig, instructionType) {
    private var nameAssigned = false

    internal var declId: String = ""
    var name: String
        get() {
            if (!nameAssigned) {
                stepConfig.stepName = stepConfig.environment.uniqueName("!${instructionType.localName}")
                nameAssigned = true
            }
            return stepConfig.stepName
        }
        set(value) {
            checkOpen()
            nameAssigned = true
            stepConfig.stepName = value
        }

    val location: Location
        get() = stepConfig.location

    internal var timeout = Duration.ZERO
    internal var depends = mutableSetOf<String>()
    internal val _staticOptions = mutableMapOf<QName, StaticOptionDetails>()
    val staticOptions: Map<QName, StaticOptionDetails>
        get() = _staticOptions

    // ========================================================================================

    protected fun elaborateInstructionInfo() {
        for (name in depends) {
            val dependsOn = stepConfig.inscopeStepNames[name] ?: throw stepConfig.exception(XProcError.xsDependsNotAStep(name))

            // Generally, the children of a container are in scope. But that's not true for [p:]depends
            var p: XProcInstruction? = dependsOn
            while (p != null) {
                if (p === this) {
                    throw stepConfig.exception(XProcError.xsDependsNotAllowed(name))
                }
                p = p.parent
            }

            val depout = if (dependsOn.namedOutput("!depends") == null) {
                val wo = dependsOn.withOutput()
                wo._port = "!depends"
                wo.sequence = true
                wo.primary = false
                wo
            } else {
                dependsOn.namedOutput("!depends")!!
            }
            val depin = withInput()
            depin._port = "!depends"
            depin.sequence = true
            depin.primary = false

            val pipe = depin.pipe()
            pipe.setReadablePort(depout)
        }
    }

    open fun checkInputBindings() {
        for (child in children.filterIsInstance<InputBindingInstruction>()) {
            if (!child.weldedShut && (child.children.isEmpty() && child.defaultBindings.isEmpty())) {
                throw stepConfig.exception(XProcError.xsNotConnected(child.port))
            }
        }
    }

    override fun elaborateInstructions() {
        elaborateInstructionInfo()
        super.elaborateInstructions()
        checkInputBindings()

        if (stepConfig.xmlCalabash.xmlCalabashConfig.assertions != AssertionsLevel.IGNORE) {
            AssertionsMonitor.parseStepAssertions(this)
        }
    }

    open fun rewriteBindings(): List<AtomicStepInstruction> {
        val result = mutableListOf<AtomicStepInstruction>()
        for (input in inputs()) {
            var selectStep: AtomicSelectStepInstruction? = null
            if (input.select != null) {
                selectStep = AtomicSelectStepInstruction(this.parent!!, input.select!!)
                val source = selectStep.withInput()
                source.port = "source"
                result.add(selectStep)
                variableBindings(input.select!!, selectStep)
            }

            val newChildren = mutableListOf<PipeInstruction>()
            for (child in input.children) {
                val binding = child as ConnectionInstruction
                when (binding) {
                    is PipeInstruction -> {
                        newChildren.add(binding)
                    }
                    else -> {
                        val steps = binding.promoteToStep(this.parent!! as StepDeclaration, this)
                        val readablePort = steps.last().primaryOutput()!!
                        val pipe = PipeInstruction(this)
                        pipe.setReadablePort(readablePort)
                        newChildren.add(pipe)
                        result.addAll(steps)
                    }
                }
            }

            if (selectStep != null) {
                val selInput = selectStep.namedInput("source")!!
                for (child in newChildren) {
                    val pipe = selInput.pipe()
                    pipe.step = child.step
                    pipe.port = child.port
                    if (child.readablePort != null) {
                        pipe.setReadablePort(child.readablePort!!)
                    }
                }

                selectStep.elaborateAtomicStep()

                val readablePort = selectStep.primaryOutput()!!
                val pipe = PipeInstruction(this)
                pipe.setReadablePort(readablePort)
                newChildren.clear()
                newChildren.add(pipe)
            }

            input._children.clear()
            input._children.addAll(newChildren)
        }

        val options = mutableListOf<WithOptionInstruction>()
        options.addAll(children.filterIsInstance<WithOptionInstruction>())

        for (option in options) {
            if (option.canBeResolvedStatically()) {
                _staticOptions[option.name] = builder.staticOptionsManager.get(option)
            } else {
                val exprStep = AtomicExpressionStepInstruction(this, option.select!!)
                exprStep.depends.addAll(depends)
                stepConfig.addVisibleStepName(exprStep)

                if (option.select!!.contextRef) {
                    val wi = exprStep.withInput()
                    wi.port = "source"
                    for (child in option.children) {
                       val binding = child as ConnectionInstruction
                        when (binding) {
                            is PipeInstruction -> {
                                val pipe = wi.pipe()
                                pipe.setReadablePort(binding.readablePort!!)
                            }
                            else -> {
                                val steps = binding.promoteToStep(this.parent!! as StepDeclaration, this)
                                val readablePort = steps.last().primaryOutput()!!
                                val pipe = wi.pipe(this)
                                pipe.setReadablePort(readablePort)
                                result.addAll(steps)
                            }
                        }
                    }
                }

                variableBindings(option.select!!, exprStep)

                exprStep.elaborateAtomicStep()
                result.add(exprStep)
                val wi = withInput()
                wi._port = "Q{${option.name.namespaceUri}}${option.name.localName}"
                val readablePort = exprStep.primaryOutput()!!
                val pipe = wi.pipe()
                pipe.setReadablePort(readablePort)
            }
        }

        return result
    }

    open protected fun variableBindings(expr: XProcExpression, step: StepDeclaration) {
        for (name in expr.variableRefs) {
            if (name !in step.stepConfig.inscopeVariables) {
                throw stepConfig.exception(XProcError.xsXPathStaticError(name))
            }

            if (!step.stepConfig.inscopeVariables[name]!!.canBeResolvedStatically()) {
                // What if it hasn't been promoted yet?
                val exprStep = step.stepConfig.inscopeVariables[name]!!.exprStep!!
                val wi = step.withInput()
                wi._port = "Q{${name.namespace}}${name.localName}"
                wi.primary = false
                wi.sequence = false
                val pipe = wi.pipe()
                pipe.setReadablePort(exprStep.primaryOutput()!!)
            }
        }
    }

    fun depends(steplist: String) {
        if (steplist.trim().isEmpty()) {
            throw stepConfig.exception(XProcError.xsValueDoesNotSatisfyType(steplist, "xs:NCName+"))
        }
        for (depend in steplist.split("\\s+".toRegex())) {
            try {
                depends.add(stepConfig.parseNCName(depend))
            } catch (ex: XProcException) {
                throw ex.error.asStatic().exception()
            }
        }
    }

    fun timeout(value: String) {
        timeout = DurationUtils.parseDuration(stepConfig, value)
    }

    fun inputs(): List<PortBindingContainer> {
        val list = mutableListOf<PortBindingContainer>()
        for (child in children.filter { it is InputInstruction || it is WithInputInstruction}) {
            list.add(child as PortBindingContainer)
        }
        return list
    }

    private fun primaryPort(ports: List<PortBindingContainer>): PortBindingContainer? {
        for (port in ports) {
            if (port.primary == true) {
                return port
            }
        }
        return null
    }

    private fun namedPort(ports: List<PortBindingContainer>, name: String): PortBindingContainer? {
        for (port in ports) {
            if (port.port == name) {
                return port
            }
        }
        return null
    }

    open fun primaryInput(): PortBindingContainer? {
        return primaryPort(inputs())
    }

    fun namedInput(name: String): PortBindingContainer? {
        return namedPort(inputs(), name)
    }

    fun outputs(): List<PortBindingContainer> {
        val list = mutableListOf<PortBindingContainer>()
        for (child in children.filter { it is OutputInstruction || it is WithOutputInstruction}) {
            list.add(child as PortBindingContainer)
        }
        return list
    }

    open fun primaryOutput(): PortBindingContainer? {
        return primaryPort(outputs())
    }

    fun namedOutput(name: String): PortBindingContainer? {
        return namedPort(outputs(), name)
    }

    open fun withInput(port: String? = null): WithInputInstruction {
        val withInput = WithInputInstruction(this, stepConfig)
        if (port != null) {
            withInput._port = port
        }
        _children.add(withInput)
        return withInput
    }

    open fun withInput(): WithInputInstruction {
        val withInput = WithInputInstruction(this, stepConfig.copy())
        _children.add(withInput)
        return withInput
    }

    fun withOutput(): WithOutputInstruction {
        val withOutput = WithOutputInstruction(this, stepConfig.copy())
        _children.add(withOutput)
        return withOutput
    }

    override fun toString(): String {
        return "${instructionType} ${name}"
    }
}