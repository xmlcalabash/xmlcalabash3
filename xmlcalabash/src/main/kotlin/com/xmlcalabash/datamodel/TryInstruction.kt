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
            val group = children.filterIsInstance<GroupInstruction>().first()
            if (group.primaryOutput() != null) {
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
                is CatchInstruction -> {
                    if (child.code.isEmpty()) {
                        if (catchWithoutCode) {
                            throw XProcError.xsMultipleCatchWithoutCodes().exception()
                        }
                        catchWithoutCode = true
                    } else {
                        if (catchWithoutCode) {
                            throw XProcError.xsCatchWithoutCodesNotLast().exception()
                        }
                        for (code in child.code) {
                            if (seenCodes.contains(code)) {
                                throw XProcError.xsCatchWithDuplicateCode(code).exception()
                            }
                            seenCodes.add(code)
                        }
                    }
                    childSteps.add(child as CompoundStepDeclaration)
                    child.elaborateInstructions()
                }
                is FinallyInstruction -> {
                    if (finally != null) {
                        throw XProcError.xsTryWithMoreThanOneFinally().exception()
                    }
                    finally = child
                    child.elaborateInstructions()
                }
                is StepDeclaration -> {
                    childSteps.add(child as CompoundStepDeclaration)
                    child.elaborateInstructions()
                }
                else -> {
                    throw XProcError.xiImpossible("Unexpected child: ${child}").exception()
                }
            }
        }

        if (children.filterIsInstance<CatchInstruction>().isEmpty() && finally == null) {
            throw XProcError.xsTryWithoutCatchOrFinally().exception()
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
                    throw XProcError.xsDifferentPrimaryOutputs().exception()
                }
                if (primaryOutput != null && child.primaryOutput()!!.port != primaryOutput.port) {
                    throw XProcError.xsDifferentPrimaryOutputs().exception()
                }
            }
            for (output in child.children.filterIsInstance<OutputInstruction>()) {
                if (child is FinallyInstruction && outputs.contains(output.port)) {
                    throw XProcError.xsFinallyWithConflictingOutputs(output.port).exception()
                }
                outputs.add(output.port)
            }
        }

        if (finally != null) {
            if (finally.primaryOutput() != null) {
                throw XProcError.xsPrimaryOutputOnFinally(finally.primaryOutput()!!.port).exception()
            }
            for (output in finally.children.filterIsInstance<OutputInstruction>()) {
                if (outputs.contains(output.port)) {
                    throw XProcError.xsFinallyWithConflictingOutputs(output.port).exception()
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
        throw XProcError.xsInvalidElement(instructionType).exception()
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
            throw XProcError.xsTryWithoutSubpipeline().exception()
        }
        if (children.filterIsInstance<FinallyInstruction>().isNotEmpty()) {
            throw XProcError.xsTryWithMoreThanOneFinally().exception()
        }
        val catch = CatchInstruction(this)
        _children.add(catch)
        return catch
    }

    fun finally(): FinallyInstruction {
        if (group == null) {
            throw XProcError.xsTryWithoutSubpipeline().exception()
        }
        if (children.filterIsInstance<FinallyInstruction>().isNotEmpty()) {
            throw XProcError.xsTryWithMoreThanOneFinally().exception()
        }
        val finally = FinallyInstruction(this)
        _children.add(finally)
        return finally
    }
}