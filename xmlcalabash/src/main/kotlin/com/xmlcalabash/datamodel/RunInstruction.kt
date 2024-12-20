package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

class RunInstruction(parent: XProcInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.run) {
    override val contentModel=  mapOf(NsP.withInput to '?', NsP.runInput to '*', NsP.runOption to '*', NsP.output to '*')

    override fun elaborateInstructions() {
        resolveAnonymousWithInput()

        for (option in children.filterIsInstance<RunOptionInstruction>()) {
            option.elaborateInstructions()

            // This is kind of an awful hack; we have to make Option objects for the parameters list
            val poption = OptionInstruction(option.parent!!, option.name, option.stepConfig)
            poption._select = option.select
            poption.asType = option.asType
            poption.collection = option.collection
            poption.static = option.static == true
            if (option.canBeResolvedStatically()) {
                _staticOptions[option.name] = StaticOptionDetails(option)
            }
            options.add(poption)
        }

        elaborateInstructionInfo()

        val seenPorts = mutableSetOf<String>()
        var primary: RunInputInstruction? = null
        var inputCount = 0
        for (child in children.filterIsInstance<WithInputInstruction>()) {
            if (child is RunInputInstruction) {
                if (child.port in seenPorts) {
                    throw stepConfig.exception(XProcError.xsDuplicatePortDeclaration(child.port))
                }
                seenPorts.add(child.port)
                inputCount++
                if (child.primary != false) {
                    primary = child
                }
            } else {
                child.primary = false
            }
        }
        if (primary != null && inputCount == 1) {
            primary.primary = true
        }

        configurePrimaryPorts(children.filterIsInstance<RunInputInstruction>())
        configurePrimaryPorts(children.filterIsInstance<OutputInstruction>())

        for (child in children) {
            when (child) {
                is RunOptionInstruction -> Unit
                else -> child.elaborateInstructions()
            }
        }

        checkInputBindings()
        open = false
    }

    private fun configurePrimaryPorts(list: List<PortBindingContainer>) {
        if (list.size == 1) {
            list.first().primary = list.first().primary != false
        }
        var primaryPort = ""
        for (input in list) {
            if (input.primary == true) {
                if (primaryPort != "") {
                    throw stepConfig.exception(XProcError.xiImpossible("fixme: error for duplicate primary input on run"))
                }
                primaryPort = input.port
            }
        }
    }

    fun runInput(port: String? = null): RunInputInstruction {
        val runInput = RunInputInstruction(this, stepConfig)
        if (port != null) {
            runInput._port = port
        }
        _children.add(runInput)
        return runInput
    }

    fun runInput(): RunInputInstruction {
        val runInput = RunInputInstruction(this, stepConfig.copy())
        _children.add(runInput)
        return runInput
    }

    fun runOption(name: QName): RunOptionInstruction {
        return runOption(name, null)
    }

    fun runOption(name: QName, expr: XProcExpression?): RunOptionInstruction {
        if (children.filterIsInstance<RunOptionInstruction>().any { it.name == name }) {
            throw stepConfig.exception(XProcError.xsDuplicateWithOption(name))
        }

        val runOption = RunOptionInstruction(this, name, stepConfig.copy())
        expr.let { runOption.select = it }
        _children.add(runOption)
        return runOption
    }
}