package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType

abstract class CompoundStepDeclaration(parent: XProcInstruction?, stepConfig: StepConfiguration, instructionType: QName): StepDeclaration(parent, stepConfig, instructionType) {
    internal val anySteps = mapOf(
        NsP.variable to '*',
        NsP.forEach to '*',
        NsP.viewport to '*',
        NsP.choose to '*',
        NsP.`if` to '*',
        NsP.group to '*',
        NsP.`try` to '*',
        NsP.run to '*',
        NsCx.atomicStep to '*'
    )

    abstract internal val contentModel: Map<QName, Char>
    internal val options = mutableListOf<OptionInstruction>()

    protected open fun defaultReadablePort(): PortBindingContainer? {
        val inputs = children.filterIsInstance<InputInstruction>()
        var drp: PortBindingContainer? = null
        for (input in inputs) {
            if (input.primary == true) {
                if (drp != null) {
                    throw XProcError.xsMultiplePrimaryInputPorts(input.port).exception()
                }
                drp = input
            }
        }
        return drp
    }

    open fun checkImplicitOutput(lastStep: StepDeclaration) {
        if (lastStep.primaryOutput() == null) {
            return
        }

        if (outputs().isEmpty()) {
            val output = OutputInstruction(this, "!result", true, true)
            _children.add(0, output)
        }
    }

    protected fun findStepNames(stepNames: Map<String, StepDeclaration>): Map<String, StepDeclaration> {
        val newStepNames = mutableMapOf<String, StepDeclaration>()
        newStepNames.putAll(stepNames)
        for (child in children.filterIsInstance<StepDeclaration>()) {
            when (child) {
                is DeclareStepInstruction -> Unit
                //is WhenInstruction, is OtherwiseInstruction -> Unit
                //is CatchInstruction, is FinallyInstruction -> Unit
                else -> {
                    if (newStepNames.containsKey(child.name)) {
                        throw XProcError.xsDuplicateStepName(child.name).exception()
                    }
                    newStepNames[child.name] = child
                    stepConfig.addVisibleStepName(child)
                }
            }
        }
        return newStepNames
    }

    protected fun coherentPortDeclarations(portList: List<PortBindingContainer>, error: (String) -> XProcError) {
        var primary: PortBindingContainer? = null
        val portNames = mutableSetOf<String>()
        for (io in portList) {
            if (portNames.contains(io.port)) {
                throw XProcError.xsDuplicatePortName(io.port).exception()
            }
            portNames.add(io.port)
            if (portList.size == 1) {
                io._primary = io._primary != false
            }
            if (io.primary == true) {
                if (primary != null) {
                    throw error(io.port).exception()
                }
                primary = io
            }
        }
    }

    protected fun findAlternativeStepDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        updateStepConfig(stepTypes, stepNames, bindings)

        // Is the externally visible structure of this declaration coherent?
        coherentPortDeclarations(children.filterIsInstance<OutputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryOutputPorts(port) })

        val newStepNames = mutableMapOf<String, StepDeclaration>()
        newStepNames.putAll(stepNames)

        val newBindings = mutableMapOf<QName, VariableBindingContainer>()
        newBindings.putAll(bindings)

        for (child in children) {
            if (child is StepDeclaration) {
                // You can see yourself, but not your siblings
                newStepNames.put(child.name, child)
                child.findDeclarations(stepTypes, newStepNames, newBindings)
                newStepNames.remove(child.name)
            } else {
                child.findDeclarations(stepTypes, newStepNames, newBindings)
            }
        }

        findOutputDeclarations()
    }

    override fun findDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        updateStepConfig(stepTypes, stepNames, bindings)

        // Is the externally visible structure of this declaration coherent?
        coherentPortDeclarations(children.filterIsInstance<OutputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryOutputPorts(port) })

        val newStepNames = findStepNames(stepNames)

        val newBindings = mutableMapOf<QName, VariableBindingContainer>()
        newBindings.putAll(bindings)

        var lastChild: VariableBindingContainer? = null
        for (child in children) {
            if (lastChild != null) {
                newBindings[lastChild.name] = lastChild
            }

            lastChild = null
            child.findDeclarations(stepTypes, newStepNames, newBindings)

            if (child is VariableBindingContainer) {
                lastChild = child
            }
        }

        findOutputDeclarations()
    }

    open fun findOutputDeclarations(): OutputInstruction? {
        if (children.filterIsInstance<OutputInstruction>().isNotEmpty()) {
            return null
        }

        val lastStep = children.filterIsInstance<StepDeclaration>().lastOrNull() ?: return null
        val lastOutput = lastStep.primaryOutput() ?: return null

        // Don't use output() because we may be putting this in a step where the user isn't
        // allowed to, for example in p:choose
        val output = OutputInstruction(this)
        output._port = "!result"
        output._primary = true
        output._sequence = true // implicitly created ports always allow sequences
        output._contentTypes.addAll(lastOutput.contentTypes)

        // Make it first
        _children.remove(output)
        _children.add(0, output)

        // N.B. Don't connect it; elaborateDeclarations will do that, and it may need to be different
        // in different context (e.g., in p:choose or p:try)

        return output
    }

    override fun findDefaultReadablePort(drp: PortBindingContainer?) {
        stepConfig.drp = drp

        for (child in _children.filterIsInstance<InputBindingInstruction>()) {
            child.findDefaultReadablePort(drp)
        }

        var curdrp = defaultReadablePort() ?: drp

        for (child in _children) {
            when (child) {
                is InputBindingInstruction -> Unit
                is OutputInstruction -> Unit
                is DeclareStepInstruction -> child.findDefaultReadablePort(null)
                is StepDeclaration -> {
                    child.findDefaultReadablePort(curdrp)
                    curdrp = child.primaryOutput()
                }
                else -> child.findDefaultReadablePort(curdrp)
            }
        }

        for (child in _children.filterIsInstance<OutputInstruction>()) {
            child.findDefaultReadablePort(curdrp)
        }

    }

    override fun elaborateInstructions() {
        if (children.filterIsInstance<StepDeclaration>().isEmpty()) {
            throw XProcError.xsNoSteps().exception()
        }

        for (option in children.filterIsInstance<OptionInstruction>()) {
            option.elaborateInstructions()
            if (option.static) {
                _staticOptions[option.name] = builder.staticOptionsManager.get(option)
            }
        }

        elaborateInstructionInfo()

        for (child in children) {
            when (child) {
                is OutputInstruction -> Unit // see below
                is OptionInstruction -> {
                    // Elaborated above
                    options.add(child)
                }
                else -> child.elaborateInstructions()
            }
        }

        val lastStep = children.filterIsInstance<StepDeclaration>().last()
        checkImplicitOutput(lastStep)

        for (output in outputs()) {
            val result = lastStep.primaryOutput()
            if (output.primary == true && output.children.isEmpty() && output.pipe == null && output.href == null) {
                if (result == null) {
                    throw XProcError.xsNoOutputConnection(output.port).exception()
                } else {
                    val pipe = output.pipe()
                    pipe.setReadablePort(result)
                }
            }
            output.elaborateInstructions()
        }

        checkInputBindings()
        open = false
    }

    protected fun sinkUnreadPorts() {
        val unreadOutputs = mutableSetOf<PortBindingContainer>()

        for (child in children.filterIsInstance<StepDeclaration>()) {
            unreadOutputs.addAll(child.children.filterIsInstance<WithOutputInstruction>())
            unreadOutputs.addAll(child.children.filterIsInstance<OutputInstruction>())
        }

        findReaders(unreadOutputs, this)

        for (wo in unreadOutputs) {
            sinkUnreadablePort(wo)
        }
    }

    private fun findReaders(readableOutputs: MutableSet<PortBindingContainer>, step: CompoundStepDeclaration) {
        for (child in step.children.filterIsInstance<OutputInstruction>()) {
            for (pipe in child.children.filterIsInstance<PipeInstruction>()) {
                readableOutputs.remove(pipe.readablePort)
            }
        }

        for (child in step.children.filterIsInstance<StepDeclaration>()) {
            for (wi in child.children.filterIsInstance<WithInputInstruction>()) {
                for (pipe in wi.children.filterIsInstance<PipeInstruction>()) {
                    readableOutputs.remove(pipe.readablePort)
                }
            }
            if (child is CompoundStepDeclaration) {
                findReaders(readableOutputs, child)
            }
        }
    }

    protected open fun sinkUnreadablePort(wo: PortBindingContainer) {
        val sink = atomicStep(NsP.sink)
        val wi = sink.withInput()
        wi.port = "source"
        wi.sequence = true
        val pipe = wi.pipe()
        pipe.setReadablePort(wo)
        sink.elaborateInstructions()
    }

    internal open fun rewrite() {
        val newChildren = mutableListOf<XProcInstruction>()
        for (child in children) {
            when (child) {
                is VariableInstruction -> {
                    if (!child.canBeResolvedStatically()) {
                        val newSteps = child.promoteToStep(this)
                        val last = newSteps.last()
                        child._withOutput = last.primaryOutput() as WithOutputInstruction
                        newChildren.addAll(newSteps)
                    }
                }
                is OptionInstruction -> {
                    if (!child.static) {
                        // Always make steps for options so they can be overridden
                        val newSteps = child.promoteToStep(this)
                        val last = newSteps.last()
                        child._withOutput = last.primaryOutput() as WithOutputInstruction
                        newChildren.addAll(newSteps)
                    }
                }
                is OutputInstruction -> {
                    newChildren.add(child)
                    val newBindings = mutableListOf<ConnectionInstruction>()
                    for (gchild in child.children) {
                        val binding = gchild as ConnectionInstruction
                        when (binding) {
                            is PipeInstruction -> {
                                newBindings.add(binding)
                            }
                            else -> {
                                val steps = binding.promoteToStep(this as StepDeclaration, this)
                                val readablePort = steps.last().primaryOutput()!!
                                val pipe = PipeInstruction(this)
                                pipe.setReadablePort(readablePort)
                                newBindings.add(pipe)
                                newChildren.addAll(steps)
                            }
                        }
                    }
                    child._children.clear()
                    child._children.addAll(newBindings)
                }

                is StepDeclaration -> {
                    val newSteps = child.rewriteBindings()
                    newChildren.addAll(newSteps)
                    newChildren.add(child)

                    if (child is CompoundStepDeclaration) {
                        child.rewrite()
                    }

                }
                else -> {
                    newChildren.add(child)
                }
            }
        }

        _children.clear()
        _children.addAll(newChildren)
    }

    protected fun resolveAnonymousWithInput() {
        var withInput = children.filterIsInstance<WithInputInstruction>().firstOrNull()
        if (withInput == null) {
            withInput = withInput()
            withInput._port = "!source"
            withInput.sequence = true
            // Make it first
            _children.remove(withInput)
            _children.add(0, withInput)
        } else {
            if (withInput.portDefined) {
                if (withInput._port != "!source") {
                    throw XProcError.xsPortNameNotAllowed().exception()
                }
            } else {
                withInput._port = "!source"
            }
            withInput.sequence = true
        }
    }

    protected fun addInstruction(instruction: XProcInstruction, type: QName = instruction.instructionType) {
        when (contentModel[type]) {
            null, '0' -> throw XProcError.xsInvalidElement(type).exception()
            '?', '1' -> {
                if (children.any { it.instructionType == type }) {
                    throw XProcError.xsInvalidElement(type).exception()
                } else {
                    _children.add(instruction)
                }
            }
            '*' -> {
                _children.add(instruction)
            }
            else -> throw XProcError.xiImpossible("Invalid content model character for {$type}: ${contentModel[type]}").exception()
        }
    }

    open fun output(port: String? = null): OutputInstruction {
        val output = OutputInstruction(this)
        port?.let { output.port = port }
        addInstruction(output)
        return output
    }

    open fun output(port: String, primary: Boolean? = null, sequence: Boolean = false): OutputInstruction {
        val output = OutputInstruction(this, port, primary, sequence)
        addInstruction(output)
        return output
    }

    open fun message(message: XProcAvtExpression) {
        val option = WithOptionInstruction(this, Ns.message, stepConfig.copy())
        option.select = message
        option.asType = SequenceType.ANY
        _children.add(option)
    }

    open fun variable(name: QName): VariableInstruction {
        val variable = VariableInstruction(this, name, stepConfig.copy())
        addInstruction(variable)
        stepConfig.addVariable(variable)
        return variable
    }

    open fun forEach(): ForEachInstruction {
        val forEach = ForEachInstruction(this)
        addInstruction(forEach)
        return forEach
    }

    open fun viewport(): ViewportInstruction {
        val viewport = ViewportInstruction(this)
        addInstruction(viewport)
        return viewport
    }

    open fun choose(): ChooseInstruction {
        val choose = ChooseInstruction(this);
        addInstruction(choose)
        return choose
    }

    open fun ifInstruction(): IfInstruction {
        val ifi = IfInstruction(this)
        addInstruction(ifi)
        return ifi
    }

    open fun group(): GroupInstruction {
        val group = GroupInstruction(this)
        addInstruction(group)
        return group
    }

    open fun tryInstruction(): TryInstruction {
        val tryi = TryInstruction(this)
        addInstruction(tryi)
        return tryi
    }

    open fun runStep(): RunInstruction {
        val run = RunInstruction(this)
        addInstruction(run)
        return run
    }

    open fun atomicStep(type: QName): AtomicStepInstruction {
        val atomic = AtomicStepInstruction(this, type)
        addInstruction(atomic, NsCx.atomicStep)
        return atomic
    }

    open fun atomicStep(type: QName, name: String): AtomicStepInstruction {
        val atomic = atomicStep(type)
        atomic.name = name
        return atomic
    }
}