package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.steps.RuntimeStepStaticContext
import net.sf.saxon.s9api.QName

interface InstructionStaticContext: RuntimeStepStaticContext {
    val inscopeStepNames: Map<String, StepDeclaration>
    val inscopeVariables: Map<QName, VariableBindingContainer>
    var drp: PortBindingContainer?

    fun addVisibleStepName(decl: StepDeclaration)
    fun addVariable(binding: VariableBindingContainer)
}