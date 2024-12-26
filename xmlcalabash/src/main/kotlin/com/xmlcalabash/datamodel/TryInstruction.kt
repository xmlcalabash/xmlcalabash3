package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

class TryInstruction(parent: XProcInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.`try`) {
    override val contentModel = emptyMap<QName,Char>() // Not used by this instruction!
    private var group: GroupInstruction? = null
    private var provisionalPrimaryOutput: PortBindingContainer? = null

    override fun findDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        findAlternativeStepDeclarations(stepTypes, stepNames, bindings)
    }

    override fun findDefaultReadablePort(drp: PortBindingContainer?) {
        stepConfig.drp = drp

        for (child in _children.filterIsInstance<InputBindingInstruction>()) {
            child.findDefaultReadablePort(drp)
        }

        val curdrp = defaultReadablePort() ?: drp

        for (child in _children) {
            when (child) {
                is InputBindingInstruction -> Unit
                is StepDeclaration -> {
                    child.findDefaultReadablePort(curdrp)
                }
                else -> child.findDefaultReadablePort(curdrp)
            }
        }

        provisionalPrimaryOutput = super.primaryOutput()
        if (provisionalPrimaryOutput == null) {
            // What if there will be one later?
            val group = children.filterIsInstance<GroupInstruction>().firstOrNull()
            if (group?.primaryOutput() != null) {
                provisionalPrimaryOutput = OutputInstruction(this, group.primaryOutput()!!.port, true, true)
            }
        }
    }

    override fun elaborateInstructions() {
        // Can't call super.elaborateInstructions() here ...

        elaborateInstructionInfo()

        for (option in children.filterIsInstance<OptionInstruction>()) {
            option.elaborateInstructions()
            if (option.canBeResolvedStatically()) {
                _staticOptions[option.name] = builder.staticOptionsManager.get(option)
            }
        }

        // Do the with-inputs first...
        var withInput = false
        for (child in children.filterIsInstance<WithInputInstruction>()) {
            child.elaborateInstructions()
            withInput = true
        }

        val childSteps = mutableListOf<CompoundStepDeclaration>()
        var finally: FinallyInstruction? = null
        var catchWithoutCode = false
        val seenCodes = mutableSetOf<QName>()

        for (child in children) {
            when (child) {
                is WithInputInstruction, is InputInstruction, is OutputInstruction -> Unit
                is WithOptionInstruction -> Unit
                is CatchInstruction -> {
                    if (child.code.isEmpty()) {
                        if (catchWithoutCode) {
                            throw stepConfig.exception(XProcError.xsMultipleCatchWithoutCodes())
                        }
                        catchWithoutCode = true
                    } else {
                        if (catchWithoutCode) {
                            throw stepConfig.exception(XProcError.xsCatchWithoutCodesNotLast())
                        }
                        for (code in child.code) {
                            if (seenCodes.contains(code)) {
                                throw stepConfig.exception(XProcError.xsCatchWithDuplicateCode(code))
                            }
                            seenCodes.add(code)
                        }
                    }
                    childSteps.add(child as CompoundStepDeclaration)
                    child.elaborateInstructions()
                }
                is FinallyInstruction -> {
                    if (finally != null) {
                        throw stepConfig.exception(XProcError.xsTryWithMoreThanOneFinally())
                    }
                    finally = child
                    child.elaborateInstructions()
                }
                is StepDeclaration -> {
                    childSteps.add(child as CompoundStepDeclaration)
                    child.elaborateInstructions()
                }
                else -> {
                    throw stepConfig.exception(XProcError.xiImpossible("Unexpected child: ${child}"))
                }
            }
        }

        if (children.filterIsInstance<CatchInstruction>().isEmpty() && finally == null) {
            throw stepConfig.exception(XProcError.xsTryWithoutCatchOrFinally())
        }

        val outputs = mutableSetOf<String>()
        var primaryOutput: PortBindingContainer? = null
        var first = true
        for (child in childSteps) {
            if (first) {
                primaryOutput = child.primaryOutput()
                first = false
            } else {
                if ((primaryOutput == null && child.primaryOutput() != null)
                    || (primaryOutput != null && child.primaryOutput() == null)) {
                    throw stepConfig.exception(XProcError.xsDifferentPrimaryOutputs())
                }
                if (primaryOutput != null && child.primaryOutput()!!.port != primaryOutput.port) {
                    throw stepConfig.exception(XProcError.xsDifferentPrimaryOutputs())
                }
            }
            for (output in child.children.filterIsInstance<OutputInstruction>()) {
                if (child is FinallyInstruction && outputs.contains(output.port)) {
                    throw stepConfig.exception(XProcError.xsFinallyWithConflictingOutputs(output.port))
                }
                outputs.add(output.port)
            }
        }

        if (finally != null) {
            if (finally.primaryOutput() != null) {
                throw stepConfig.exception(XProcError.xsPrimaryOutputOnFinally(finally.primaryOutput()!!.port))
            }
            for (output in finally.children.filterIsInstance<OutputInstruction>()) {
                if (outputs.contains(output.port)) {
                    throw stepConfig.exception(XProcError.xsFinallyWithConflictingOutputs(output.port))
                }
                outputs.add(output.port)
            }
        }

        for (portName in outputs) {
            var addOutput = true
            val output = if (primaryOutput != null && portName == primaryOutput.port) {
                if (super.primaryOutput() != null) {
                    addOutput = false
                    primaryOutput()!!
                } else {
                    addOutput = false
                    _children.add(0, provisionalPrimaryOutput!!)
                    provisionalPrimaryOutput!!
                }
            } else {
                OutputInstruction(this, portName, primaryOutput != null && portName == primaryOutput.port, true)
            }

            // All the child steps of the p:try are in scope for this output...
            for (child in children.filterIsInstance<StepDeclaration>()) {
                output.stepConfig.addVisibleStepName(child)
            }

            for (child in children.filterIsInstance<CompoundStepDeclaration>()) {
                val stepoutput = child.namedOutput(portName)
                if (stepoutput != null) {
                    val pipe = output.pipe()
                    pipe.setReadablePort(stepoutput)
                }
            }

            output.elaborateInstructions()

            if (addOutput) {
                _children.add(0, output)
            }
        }
    }

    override fun sinkUnreadablePort(wo: PortBindingContainer) {
        // Don't put the sink inside the group
        val sink = AtomicStepInstruction(this, NsP.sink)
        _children.add(sink)

        val wi = sink.withInput()
        wi.port = "source"
        val pipe = wi.pipe()
        pipe.setReadablePort(wo)
        sink.elaborateInstructions()
    }

    override fun primaryOutput(): PortBindingContainer? {
        return provisionalPrimaryOutput
    }

    override fun withInput(port: String?): WithInputInstruction {
        throw stepConfig.exception(XProcError.xsInvalidElement(instructionType))
    }

    override fun output(port: String?): OutputInstruction {
        group = group ?: tryGroup()
        return group!!.output(port)
    }

    override fun output(port: String, primary: Boolean?, sequence: Boolean): OutputInstruction {
        group = group ?: tryGroup()
        return group!!.output(port, primary, sequence)
    }

    override fun forEach(): ForEachInstruction {
        group = group ?: tryGroup()
        return group!!.forEach()
    }

    override fun viewport(): ViewportInstruction {
        group = group ?: tryGroup()
        return group!!.viewport()
    }

    override fun choose(): ChooseInstruction {
        group = group ?: tryGroup()
        return group!!.choose()
    }

    override fun ifInstruction(): IfInstruction {
        group = group ?: tryGroup()
        return group!!.ifInstruction()
    }

    override fun group(): GroupInstruction {
        group = group ?: tryGroup()
        return group!!.group()
    }

    override fun tryInstruction(): TryInstruction {
        group = group ?: tryGroup()
        return group!!.tryInstruction()
    }

    override fun atomicStep(type: QName): AtomicStepInstruction {
        group = group ?: tryGroup()
        return group!!.atomicStep(type)
    }

    private fun tryGroup(): GroupInstruction {
        group = GroupInstruction(this)
        _children.add(group!!)
        return group!!
    }

    fun catch(): CatchInstruction {
        if (group == null) {
            throw stepConfig.exception(XProcError.xsTryWithoutSubpipeline())
        }
        if (children.filterIsInstance<FinallyInstruction>().isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsTryWithMoreThanOneFinally())
        }
        val catch = CatchInstruction(this)
        _children.add(catch)
        return catch
    }

    fun finally(): FinallyInstruction {
        if (group == null) {
            throw stepConfig.exception(XProcError.xsTryWithoutSubpipeline())
        }
        if (children.filterIsInstance<FinallyInstruction>().isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsTryWithMoreThanOneFinally())
        }
        val finally = FinallyInstruction(this)
        _children.add(finally)
        return finally
    }
}