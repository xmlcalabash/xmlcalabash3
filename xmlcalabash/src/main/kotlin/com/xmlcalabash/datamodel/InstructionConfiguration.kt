package com.xmlcalabash.datamodel

import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

class InstructionConfiguration(saxonConfig: SaxonConfiguration,
                               context: DocumentContext,
                               environment: XProcEnvironment): StepConfiguration(saxonConfig, context, environment) {
    companion object {
        internal var _id = 0L
    }

    constructor(config: InstructionConfiguration, context: DocumentContext): this(config.saxonConfig, context, config.environment)

    private val _inscopeStepNames = mutableMapOf<String, StepDeclaration>()
    private val _inscopeVariables = mutableMapOf<QName, VariableBindingContainer>()
    private val _staticBindings = mutableMapOf<QName, XdmValue>()

    val inscopeStepNames: Map<String, StepDeclaration> = _inscopeStepNames
    val inscopeVariables: Map<QName, VariableBindingContainer> = _inscopeVariables
    val staticBindings: Map<QName, XdmValue> = _staticBindings
    val nextId: String = synchronized(Companion) {
        "IC${++_id}"
    }
    var drp: PortBindingContainer? = null
    var eagerEvaluation = environment.xmlCalabashConfig.eagerEvaluation

    override fun copy(): InstructionConfiguration {
        return copy(saxonConfig, context.copy())
    }

    fun copyNew(): InstructionConfiguration {
        val newConfig = saxonConfig.newConfiguration()
        val newContext = DocumentContextImpl(newConfig)
        newContext.updateWith(context.location)
        newContext.updateWith(context.inscopeNamespaces)
        return copy(newConfig, newContext)
    }

    private fun copy(sconfig: SaxonConfiguration, ccontext: DocumentContext): InstructionConfiguration {
        val iconfig = InstructionConfiguration(sconfig, ccontext, this.environment)
        iconfig._inscopeStepNames.putAll(_inscopeStepNames)
        iconfig._inscopeVariables.putAll(_inscopeVariables)
        iconfig._staticBindings.putAll(_staticBindings)
        iconfig._inscopeStepTypes.putAll(_inscopeStepTypes)
        iconfig.drp = drp
        return iconfig
    }

    fun addVisibleStepName(decl: StepDeclaration) {
        val name = decl.name
        val current = _inscopeStepNames[name]
        if (current != null && current !== decl) {
            throw exception(XProcError.xsDuplicateStepName(name))
        }
        _inscopeStepNames[name] = decl
    }

    fun addVisibleStepType(decl: DeclareStepInstruction) {
        val name = decl.type!!
        val current = inscopeStepTypes[name]
        if (current != null && current !== decl) {
            throw exception(XProcError.xsDuplicateStepType(name))
        }
        putStepType(name, decl)
    }

    fun addVariable(binding: VariableBindingContainer) {
        _inscopeVariables[binding.name] = binding
    }

    fun addStaticBinding(name: QName, value: XdmValue) {
        _staticBindings[name] = value
    }

    override fun with(prefix: String, uri: NamespaceUri): InstructionConfiguration {
        val ccontext = context.with(prefix, uri)
        return copy(saxonConfig, ccontext)
    }

    override fun with(location: Location): InstructionConfiguration {
        val ccontext = context.with(location)
        return copy(saxonConfig, ccontext)
    }
}