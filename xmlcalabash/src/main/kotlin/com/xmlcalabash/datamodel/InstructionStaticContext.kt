package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.steps.RuntimeStepStaticContext
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

interface InstructionStaticContext: RuntimeStepStaticContext {
    val inscopeStepNames: Map<String, StepDeclaration>
    val inscopeVariables: Map<QName, VariableBindingContainer>
    val staticBindings: Map<QName, XdmValue>
    var drp: PortBindingContainer?

    fun addVisibleStepName(decl: StepDeclaration)
    fun addVariable(binding: VariableBindingContainer)
    fun addStaticBinding(binding: QName, value: XdmValue)
}