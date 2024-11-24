package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.runtime.XProcExecutionContext
import net.sf.saxon.s9api.QName

class ConditionalExecutionContext(stepConfig: StepConfiguration, val context: UseWhenContext): XProcExecutionContext(stepConfig) {
    override fun stepAvailable(name: QName): Boolean {
        if (context.stepTypes.contains(name)) {
            val impl = context.stepTypes[name]!!
            if (impl.resolved) {
                //println("${name} resolved in ${context}: ${impl.isImplemented}")
                return impl.isImplemented
            }
            //println("${name} unresolved in ${context}")
            throw ConditionalStepException("${name}")
        }
        if (context.unknownStepTypes) {
            //println("${name} unresolved in ${context}")
            throw ConditionalStepException("${name}")
        }
        //println("${name} resolved in ${context}: false")
        return false
    }
}