package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

open class ChooseInstruction(parent: XProcInstruction, tag: QName = NsP.choose): CompoundStepDeclaration(parent, parent.stepConfig.copy(), tag) {
    override val contentModel = mapOf(NsP.withInput to '1', NsP.`when` to '*', NsP.otherwise to '?')

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
    }

    override fun findDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        findAlternativeStepDeclarations(stepTypes, stepNames, bindings)
    }

    override fun findOutputDeclarations(): OutputInstruction? {
        val output = super.findOutputDeclarations()
        if (output != null) {
            val lastOutput = children.filterIsInstance<StepDeclaration>().last().primaryOutput()!!
            output._port = lastOutput.port
        }
        return output
    }

    override fun elaborateInstructions() {
        // Can't call super.elaborateInstructions() here ...

        resolveAnonymousWithInput()
        elaborateInstructionInfo()

        // Make this one "by hand" because p:choose can't have input instructions
        val current = InputInstruction(this, "!context", true, true)
        _children.add(1, current)

        for (option in children.filterIsInstance<OptionInstruction>()) {
            if (option.canBeResolvedStatically()) {
                _staticOptions[option.name] = builder.staticOptionsManager.get(option)
            }
        }

        var hasContext = true
        val withInput = children.filterIsInstance<WithInputInstruction>().first()
        withInput.sequence = true
        val connected = withInput.children.isNotEmpty() || withInput.href != null || withInput.pipe != null
        if (!connected) {
            if (stepConfig.drp == null) {
                val empty = withInput.empty()
                hasContext = false
            } else {
                val pipe = withInput.pipe()
                pipe.setReadablePort(stepConfig.drp!!, true)
            }
        }
        withInput.elaborateInstructions()

        val exprContextInput = children.filterIsInstance<InputInstruction>().first()
        if (!hasContext) {
            _children.remove(exprContextInput)
        }

        for (child in children) {
            when (child) {
                is WithInputInstruction, is InputInstruction, is OutputInstruction -> Unit
                is WithOptionInstruction -> Unit
                is StepDeclaration -> {
                    child.elaborateInstructions()
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected child: ${child}"))
            }
        }

        if (children.filterIsInstance<CompoundStepDeclaration>().isEmpty()) {
            throw stepConfig.exception(XProcError.xsWhenOrOtherwiseRequired())
        }

        val outputs = mutableSetOf<String>()
        var primaryOutput: PortBindingContainer? = null
        var first = true
        for (child in children.filterIsInstance<CompoundStepDeclaration>()) {
            if (first) {
                primaryOutput = child.primaryOutput()
                first = false
            } else {
                if (primaryOutput == null) {
                    if (child.primaryOutput() != null) {
                        throw stepConfig.exception(XProcError.xsDifferentPrimaryOutputs())
                    }
                } else {
                    if (child.primaryOutput() == null) {
                        throw stepConfig.exception(XProcError.xsDifferentPrimaryOutputs())
                    }
                    if (child.primaryOutput() == null || child.primaryOutput()!!.port != primaryOutput.port) {
                        throw stepConfig.exception(XProcError.xsDifferentPrimaryOutputs())
                    }
                }
            }
            for (output in child.children.filterIsInstance<OutputInstruction>()) {
                outputs.add(output.port)
            }
        }

        if (children.filterIsInstance<OtherwiseInstruction>().isEmpty()) {
            if (primaryOutput != null && stepConfig.drp != null) {
                val otherwise = otherwise()
                stepConfig.addVisibleStepName(otherwise)
                otherwise.output(primaryOutput.port, primary = true, sequence = true)
                val identity = otherwise.atomicStep(NsP.identity)
                val wi = identity.withInput()
                wi.port = "source"
                val pipe = wi.pipe()
                pipe.setReadablePort(stepConfig.drp!!, true)
                identity.elaborateAtomicStep()
                otherwise.elaborateInstructions()
            }
        }

        // This is a bit messy because the *primary* output might already have been added
        for (portName in outputs) {
            // Is this the default primary output that got added by CompoundStepDeclaration?
            var addOutput = true
            val output = if (primaryOutput != null && portName == primaryOutput.port) {
                if (primaryOutput() == null) {
                    OutputInstruction(this, portName, true, true)
                } else {
                    addOutput = false
                    primaryOutput()!!
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
                    pipe.setReadablePort(stepoutput, true)
                }
            }

            output.elaborateInstructions()
            output._sequence = true
            output._contentTypes.clear()

            if (addOutput) {
                _children.add(0, output)
            }
        }
    }

    fun whenInstruction(): WhenInstruction {
        val wi = WhenInstruction(this)
        addInstruction(wi)
        return wi
    }

    fun otherwise(): OtherwiseInstruction {
        val otherwise = OtherwiseInstruction(this)
        addInstruction(otherwise)
        return otherwise
    }
}