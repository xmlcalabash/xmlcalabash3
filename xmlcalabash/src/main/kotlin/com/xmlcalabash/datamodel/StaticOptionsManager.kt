package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

// We have to maintain the name/value mappings for use during use-when analysis.
// It's not clear at the moment (28 Nov 2024) if that should replace the current
// options processing or not. They're going to run in parallel for the moment.

class StaticOptionsManager() {
    private val options = mutableMapOf<VariableBindingContainer, StaticOptionDetails>()
    private val _useWhenOptions = mutableMapOf<QName, XdmValue>()
    private val compileTimeOptions = mutableMapOf<QName,XProcExpression>()
    private val _staticOptions = mutableSetOf<QName>()

    val useWhenOptions: Map<QName,XdmValue>
        get() = _useWhenOptions

    val staticOptions: Set<QName> = _staticOptions

    fun compileTimeValue(name: QName, value: XProcExpression) {
        compileTimeOptions[name] = value
        _useWhenOptions[name] = value.evaluate(value.stepConfig)
    }

    fun useWhenValue(name: QName, value: XdmValue) {
        _useWhenOptions[name] = value
    }

    fun get(variable: VariableBindingContainer): StaticOptionDetails {
        if (!options.containsKey(variable)) {
            val details = when (variable) {
                is OptionInstruction -> {
                    if (variable.static == true) {
                        _staticOptions.add(variable.name)
                    }
                    StaticOptionDetails(variable)
                }
                is WithOptionInstruction -> StaticOptionDetails(variable)
                is VariableInstruction -> StaticOptionDetails(variable)
                else -> throw XProcError.xiImpossible("Unexpected static option type").exception()
            }

            if (compileTimeOptions.containsKey(variable.name)) {
                details.override(compileTimeOptions[variable.name]!!.evaluate(variable.stepConfig))
            }
            options[variable] = details
        }
        return options[variable]!!
    }
}