package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

class StaticOptionsManager() {
    private val options = mutableMapOf<VariableBindingContainer, StaticOptionDetails>()
    private val compileTimeOptions = mutableMapOf<QName,XProcExpression>()

    internal fun copy(): StaticOptionsManager {
        val manager = StaticOptionsManager()
        manager.options.putAll(options)
        manager.compileTimeOptions.putAll(compileTimeOptions)
        return manager
    }

    fun compileTimeValue(name: QName, value: XProcExpression) {
        compileTimeOptions[name] = value
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