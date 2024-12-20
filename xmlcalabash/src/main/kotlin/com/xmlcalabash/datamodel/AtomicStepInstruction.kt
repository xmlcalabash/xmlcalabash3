package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import javax.lang.model.type.ArrayType

open class AtomicStepInstruction(parent: XProcInstruction, instructionType: QName): StepDeclaration(parent, parent.stepConfig.copy(), instructionType) {
    fun elaborateAtomicStep() {
        findDeclarations(stepConfig.inscopeStepTypes, stepConfig.inscopeStepNames, stepConfig.inscopeVariables)
        elaborateInstructions()
        parent!!.stepConfig.addVisibleStepName(this)
    }

    fun declaration(): DeclareStepInstruction? {
        return findStepDeclaration(instructionType)
    }

    override fun findDeclarations(
        stepTypes: Map<QName, DeclareStepInstruction>,
        stepNames: Map<String, StepDeclaration>,
        bindings: Map<QName, VariableBindingContainer>
    ) {
        super.findDeclarations(stepTypes, stepNames, bindings)

        val decl = declaration()
            ?: throw stepConfig.exception(XProcError.xsMissingStepDeclaration(instructionType))

        declId = decl.id
        val seenPorts = mutableSetOf<String>()
        for (input in children.filterIsInstance<WithInputInstruction>()) {
            if (input.port.startsWith("Q{")) {
                continue
            }

            val dinput = if (!input.portDefined) {
                decl.getPrimaryInput()
            } else {
                decl.getInput(input.port)
            }

            if (dinput == null) {
                if (!input.portDefined) {
                    throw stepConfig.exception(XProcError.xsNoPrimaryInput())
                } else {
                    throw stepConfig.exception(XProcError.xsNoSuchPort(input.port))
                }
            }

            if (!input.portDefined) {
                input.port = dinput.port
            }

            if (seenPorts.contains(input.port)) {
                throw stepConfig.exception(XProcError.xsDuplicatePortDeclaration(input.port))
            }
            seenPorts.add(input.port)

            input._sequence = dinput.sequence
            input._primary = dinput.primary
            input._contentTypes.clear()
            input._contentTypes.addAll(dinput.contentTypes)
            input.defaultBindings.addAll(dinput.defaultBindings)
            //input.defaultSelect = dinput.select
        }

        for (input in decl.getInputs()) {
            val iport = namedInput(input.port)
            // Special case: expressions that don't have an input don't get one
            if (iport == null && instructionType != NsCx.expression) {
                val wi = withInput()
                wi.port = input.port
                wi.sequence = input.sequence
                wi.primary = input.primary
                wi.contentTypes = input.contentTypes
                wi.defaultBindings.addAll(input.defaultBindings)
                //wi.defaultSelect = input.select
            }
        }

        for (output in decl.getOutputs()) {
            val wo = withOutput()
            if (!output.portDefined) {
                output._port = "!result"
            }
            wo._port = output.port
            wo.primary = output.primary
            wo.sequence = output.sequence
            wo.contentTypes = output.contentTypes
        }

        val options = mutableMapOf<QName, WithOptionInstruction>()
        for (opt in children.filterIsInstance<WithOptionInstruction>()) {
            if ((instructionType.namespaceUri == NsP.namespace && opt.name == Ns.message)
                || (instructionType.namespaceUri != NsP.namespace && opt.name == NsP.message)) {
                continue
            }

            val dopt = decl.getOption(opt.name)
            if (dopt == null) {
                throw stepConfig.exception(XProcError.xsNoSuchOption(opt.name))
            }
            if (options.containsKey(opt.name)) {
                throw stepConfig.exception(XProcError.xsDuplicateWithOption(opt.name))
            }
            if (dopt.static) {
                throw stepConfig.exception(XProcError.xsWithOptionForStatic(opt.name))
            }
            options[opt.name] = opt
        }

        //val inscopeOptions = mutableListOf<VariableBindingContainer>()
        for (option in decl.children.filterIsInstance<OptionInstruction>()) {
            if (options.containsKey(option.name)) {
                val opt = options[option.name]!!
                opt.optionValues = option.values
                opt.asType = option.asType
                opt.specialType = option.specialType

                if (opt.initializer != null) {
                    if (option.asType == null) {
                        opt.select = XProcExpression.avt(stepConfig, opt.initializer!!)
                    } else {
                        val stype = option.asType!!.itemType.underlyingItemType
                        if (stype is MapType || stype is ArrayType) {
                            opt._select = XProcExpression.select(stepConfig, opt.initializer!!, option.asType!!, option.collection == true, option.values)
                        } else {
                            opt._select = XProcExpression.avt(stepConfig, opt.initializer!!, option.asType!!, option.values)
                        }
                    }
                    opt.initializer = null
                }
            } else {
                if (option.required == true) {
                    throw stepConfig.exception(XProcError.xsMissingRequiredOption(option.name))
                } else {
                    if (decl.isAtomic) {
                        // If the step is atomic, we have to work out all the option values.
                        // If the step isn't atomic, the p:option expressions in the subpipeline
                        // will be evaluated to work out any unspecified options.
                        val opt = withOption(option.name)
                        opt.select = option.select ?: XProcExpression.select(stepConfig, "()", SequenceType.ANY, false)
                        opt.optionValues = option.values
                        opt.asType = option.asType
                        opt.specialType = option.specialType
                    }
                }
            }
        }
    }

    // ============================================================

    open fun withOption(name: QName): WithOptionInstruction {
        return withOption(name, null)
    }

    open fun withOption(name: QName, expr: XProcExpression?): WithOptionInstruction {
        if (children.filterIsInstance<WithOptionInstruction>().any { it.name == name }) {
            throw stepConfig.exception(XProcError.xsDuplicateWithOption(name))
        }

        val withOption = WithOptionInstruction(this, name, stepConfig.copy())
        expr.let { withOption.select = it }
        _children.add(withOption)
        return withOption
    }

    open fun withOption(name: QName, value: String): WithOptionInstruction {
        return withOption(name, XProcExpression.constant(stepConfig, XdmAtomicValue(value)))
    }
}
