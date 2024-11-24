package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmValue
import javax.lang.model.type.ArrayType

open class AtomicStepInstruction(parent: XProcInstruction, instructionType: QName): StepDeclaration(parent, parent.stepConfig.copy(), instructionType) {
    private var _userDefinedStep: DeclareStepInstruction? = null
    val userDefinedStep: DeclareStepInstruction?
        get() = _userDefinedStep

    override fun validateConstruction() {
        val decl = findStepDeclaration(instructionType)
        println("VALIDATE ATOMIC")

        /*
        val decl = findStepDeclaration(instructionType)
            ?: throw XProcError.xsMissingStepDeclaration(instructionType).exception()

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
                    throw XProcError.xsNoPrimaryInput().exception()
                } else {
                    throw XProcError.xsNoSuchPort(input.port).exception()
                }
            }

            if (!input.portDefined) {
                input.port = dinput.port
            }
        }

        val options = mutableMapOf<QName, WithOptionInstruction>()
        for (opt in children.filterIsInstance<WithOptionInstruction>()) {
            val dopt = decl.getOption(opt.name)
            if (dopt == null) {
                throw XProcError.xsNoSuchOption(opt.name).exception()
            }
            options[opt.name] = opt
        }

        for (opt in decl.children.filterIsInstance<OptionInstruction>()) {
            if (options.containsKey(opt.name)) {
                val wo = options[opt.name]!!
                if (wo.initializer != null) {
                    if (opt.asType == null) {
                        wo.select = XProcExpression.avt(stepConfig, wo.initializer!!)
                    } else {
                        val stype = opt.asType!!.itemType.underlyingItemType
                        if (stype is MapType || stype is ArrayType) {
                            wo._select = XProcExpression.select(stepConfig, wo.initializer!!, opt.asType!!, opt.collection == true, opt.values)
                        } else {
                            wo._select = XProcExpression.avt(stepConfig, wo.initializer!!, opt.asType!!, opt.values)
                        }
                    }
                    wo.initializer = null
                }
            }
        }
         */

        for (child in children) {
            child.validateConstruction()
        }

        super.validateConstruction()
        open = false
    }

    /*
    override fun elaborate() {
        val decl = findStepDeclaration(instructionType)!!

        for (input in children.filterIsInstance<WithInputInstruction>()) {
            if (input.port.startsWith("Q{")) {
                continue
            }

            val dinput = decl.getInput(input.port)!!

            input._sequence = dinput.sequence
            input._primary = dinput.primary
            input._contentTypes.clear()
            input._contentTypes.addAll(dinput.contentTypes)
        }

        for (input in decl.getInputs()) {
            val iport = namedInput(input.port)
            // Special case: expressions that don't have an input don't get one
            if (iport == null && instructionType != NsCx.expression) {
                val wi = withInput(stepConfig)
                wi.port = input.port
                wi.sequence = input.sequence
                wi.primary = input.primary
                wi.contentTypes = input.contentTypes
            }
        }

        for (output in decl.getOutputs()) {
            val wo = withOutput(stepConfig)
            wo.port = output.port
            wo.primary = output.primary
            wo.sequence = output.sequence
            wo.contentTypes = output.contentTypes
        }

        val options = mutableMapOf<QName, WithOptionInstruction>()
        for (opt in children.filterIsInstance<WithOptionInstruction>()) {
            val dopt = decl.getOption(opt.name)!!
            opt.optionValues = dopt.values
            opt._asType = dopt.asType
            opt._specialType = dopt.specialType
            options[opt.name] = opt
        }

        for (opt in decl.children.filterIsInstance<OptionInstruction>()) {
            if (!options.containsKey(opt.name)) {
                if (opt.required == true) {
                    throw XProcError.xsMissingRequiredOption(opt.name).exception()
                } else {
                    val wo = withOption(opt.name, stepConfig)
                    wo.select = opt.select ?: XProcExpression.select(stepConfig, "()", SequenceType.ANY, false)
                    wo.optionValues = opt.values
                    wo.asType = opt.asType
                    wo.specialType = opt.specialType
                }
            }
        }

        super.elaborate()
    }
     */

    override fun staticAnalysis(context: InstructionStaticContext) {
        val decl = findStepDeclaration(instructionType)
            ?: throw XProcError.xsMissingStepDeclaration(instructionType).exception()
        if (!decl.isAtomic) {
            _userDefinedStep = decl
        }

        // ==============

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
                    throw XProcError.xsNoPrimaryInput().exception()
                } else {
                    throw XProcError.xsNoSuchPort(input.port).exception()
                }
            }

            if (!input.portDefined) {
                input._port = dinput.port
            }

            input._sequence = dinput.sequence
            input._primary = dinput.primary
            input._contentTypes.clear()
            input._contentTypes.addAll(dinput.contentTypes)
        }

        for (input in decl.getInputs()) {
            val iport = namedInput(input.port)
            // Special case: expressions that don't have an input don't get one
            if (iport == null && instructionType != NsCx.expression) {
                val wi = withInput(stepConfig)
                wi.port = input.port
                wi.sequence = input.sequence
                wi.primary = input.primary
                wi.contentTypes = input.contentTypes
            }
        }

        for (output in decl.getOutputs()) {
            val wo = withOutput(stepConfig)
            wo.port = output.port
            wo.primary = output.primary
            wo.sequence = output.sequence
            wo.contentTypes = output.contentTypes
        }

        val options = mutableMapOf<QName, WithOptionInstruction>()
        for (opt in children.filterIsInstance<WithOptionInstruction>()) {
            val dopt = decl.getOption(opt.name)
            if (dopt == null) {
                throw XProcError.xsNoSuchOption(opt.name).exception()
            }
            options[opt.name] = opt
            opt.optionValues = dopt.values
            opt._asType = dopt.asType
            opt._specialType = dopt.specialType
        }

        for (opt in decl.children.filterIsInstance<OptionInstruction>()) {
            if (!options.containsKey(opt.name)) {
                if (opt.required == true) {
                    throw XProcError.xsMissingRequiredOption(opt.name).exception()
                } else {
                    val wo = withOption(opt.name, stepConfig)
                    wo.select = opt.select ?: XProcExpression.select(stepConfig, "()", SequenceType.ANY, false)
                    wo.optionValues = opt.values
                    wo.asType = opt.asType
                    wo.specialType = opt.specialType
                }
            } else {
                val wo = options[opt.name]!!
                if (wo.initializer != null) {
                    if (opt.asType == null) {
                        wo.select = XProcExpression.avt(stepConfig, wo.initializer!!)
                    } else {
                        val stype = opt.asType!!.itemType.underlyingItemType
                        if (stype is MapType || stype is ArrayType) {
                            wo._select = XProcExpression.select(stepConfig, wo.initializer!!, opt.asType!!, opt.collection == true, opt.values)
                        } else {
                            wo._select = XProcExpression.avt(stepConfig, wo.initializer!!, opt.asType!!, opt.values)
                        }
                    }
                    wo.initializer = null
                }
            }
        }

        // ===============

        super.staticAnalysis(context)

        for (input in children.filterIsInstance<WithInputInstruction>()) {
            if (input.port.startsWith("Q{") || input.port.startsWith("!depends")) {
                continue
            }

            val dinput = decl.getInput(input.port)!!
            input.defaultBindings.addAll(dinput.defaultBindings)
            input.defaultSelect = dinput.select
        }

        for (child in children) {
            child.staticAnalysis(context)
        }
    }

    // ============================================================

    internal fun withOption(name: QName, expr: XProcExpression): WithOptionInstruction {
        return withOption(name, stepConfig, expr)
    }

    internal fun withOption(name: QName, stepConfig: StepConfiguration): WithOptionInstruction {
        return withOption(name, stepConfig, null)
    }

    private fun withOption(name: QName, stepConfig: StepConfiguration, expr: XProcExpression?): WithOptionInstruction {
        if (children.filterIsInstance<WithOptionInstruction>().filter { it.name == name }.isNotEmpty()) {
            throw XProcError.xsDuplicateOption(name).exception()
        }

        val withOption = WithOptionInstruction(this, name, stepConfig)
        expr.let { withOption.select = it }
        _children.add(withOption)
        return withOption
    }


}