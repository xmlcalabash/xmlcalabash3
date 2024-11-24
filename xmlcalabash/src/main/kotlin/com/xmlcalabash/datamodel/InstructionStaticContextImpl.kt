package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.RuntimeStepStaticContextImpl
import com.xmlcalabash.runtime.steps.RuntimeStepStaticContext
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

class InstructionStaticContextImpl(val rtContext: RuntimeStepStaticContextImpl): RuntimeStepStaticContext by rtContext, InstructionStaticContext {

    private var _stepName: String? = null
    private val _inscopeStepNames = mutableMapOf<String, StepDeclaration>()
    private val _inscopeVariables = mutableMapOf<QName, VariableBindingContainer>()
    private var _drp: PortBindingContainer? = null

    override val xmlCalabash = saxonConfig.xmlCalabash
    override val processor = saxonConfig.processor

    override val inscopeStepNames: Map<String, StepDeclaration>
        get() = _inscopeStepNames
    override val inscopeVariables: Map<QName, VariableBindingContainer>
        get() = _inscopeVariables
    override var drp: PortBindingContainer?
        get() = _drp
        set(value) {
            _drp = value
        }

    fun copy(): InstructionStaticContextImpl {
        val newContext = rtContext.copy()
        return copyNew(newContext)
    }

    fun copyNew(): InstructionStaticContextImpl {
        val newContext = rtContext.copyNew()
        return copyNew(newContext)
    }

    private fun copyNew(rtContext: RuntimeStepStaticContextImpl): InstructionStaticContextImpl {
        val stepConfig = InstructionStaticContextImpl(rtContext)
        stepConfig._stepName = null
        stepConfig._inscopeStepNames.putAll(_inscopeStepNames)
        stepConfig._inscopeVariables.putAll(_inscopeVariables)
        stepConfig._drp = drp
        return stepConfig
    }

    fun with(location: Location): InstructionStaticContextImpl {
        return InstructionStaticContextImpl(rtContext.with(location))
    }

    fun with(prefix: String, uri: NamespaceUri): InstructionStaticContextImpl {
        return InstructionStaticContextImpl(rtContext.with(prefix, uri))
    }

    fun with(inscopeNamespaces: Map<String, NamespaceUri>): InstructionStaticContextImpl {
        return InstructionStaticContextImpl(rtContext.with(inscopeNamespaces))
    }

    override fun addVisibleStepName(decl: StepDeclaration) {
        val name = decl.name
        val current = _inscopeStepNames[name]
        if (current != null && current !== decl) {
            throw XProcError.xsDuplicateStepName(name).exception()
        }
        _inscopeStepNames[name] = decl
    }

    override fun addVariable(binding: VariableBindingContainer) {
        _inscopeVariables[binding.name] = binding
    }
}