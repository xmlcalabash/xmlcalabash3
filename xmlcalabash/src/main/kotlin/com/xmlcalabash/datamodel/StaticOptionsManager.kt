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

    val useWhenOptions: Map<QName,XdmValue>
        get() = _useWhenOptions

    internal fun copy(): StaticOptionsManager {
        val manager = StaticOptionsManager()
        manager.options.putAll(options)
        manager.compileTimeOptions.putAll(compileTimeOptions)
        return manager
    }

    fun compileTimeValue(name: QName, value: XProcExpression) {
        compileTimeOptions[name] = value
        _useWhenOptions[name] = value.evaluate()
    }

    fun useWhenValue(name: QName, value: XdmValue) {
        _useWhenOptions[name] = value
    }

    fun get(variable: VariableBindingContainer): StaticOptionDetails {
        if (!options.containsKey(variable)) {
            val details = when (variable) {
                is OptionInstruction -> StaticOptionDetails(variable)
                is WithOptionInstruction -> StaticOptionDetails(variable)
                is VariableInstruction -> StaticOptionDetails(variable)
                else -> throw XProcError.xiImpossible("Unexpected static option type").exception()
            }
            if (compileTimeOptions.containsKey(variable.name)) {
                details.override(compileTimeOptions[variable.name]!!.evaluate())
            }
            options[variable] = details
        }
        return options[variable]!!
    }
}