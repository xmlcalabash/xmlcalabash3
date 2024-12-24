package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

abstract class XProcInstruction internal constructor(initialParent: XProcInstruction?, val stepConfig: InstructionConfiguration, val instructionType: QName) {
    internal lateinit var builder: PipelineBuilder
    internal var _parent = initialParent

    val id = stepConfig.nextId
    var expandText: Boolean? = null

    val parent: XProcInstruction?
        get() = _parent

    init {
        if (initialParent != null) {
            builder = initialParent.builder
        }

        var p: XProcInstruction? = parent
        while (expandText == null && p != null) {
            if (p.expandText != null) {
                expandText = p.expandText
            }
            p = p.parent
        }
        expandText = expandText ?: true
    }

    internal val _children = mutableListOf<XProcInstruction>()
    val children: List<XProcInstruction>
        get() = _children

    protected val _extensionAttributes = mutableMapOf<QName, String>()
    var extensionAttributes: Map<QName, String>
        get() = _extensionAttributes
        set(value) {
            checkOpen()
            _extensionAttributes.clear()
            _extensionAttributes.putAll(value)
        }
    fun setExtensionAttribute(name: QName, value: String) {
        _extensionAttributes[name] = value
    }

    internal val _pipeinfo = mutableListOf<XdmNode>()
    val pipeinfo: List<XdmNode>
        get() = _pipeinfo

    internal var open = true
    protected fun checkOpen() {
        if (!open) {
            throw IllegalArgumentException("${instructionType} cannot be changed")
        }
    }

    val inscopeNamespaces: Map<String, NamespaceUri>
        get() = stepConfig.inscopeNamespaces

    /*
    val inscopeStepNames: Map<String, StepDeclaration>
        get() = stepConfig.inscopeStepNames

    val inscopeStepTypes: Map<QName, DeclareStepInstruction>
        get() = stepConfig.inscopeStepTypes
     */

    val inscopeVariables: Map<QName, VariableBindingContainer>
        get() = stepConfig.inscopeVariables

    fun addVisibleStepType(decl: DeclareStepInstruction) {
        stepConfig.addVisibleStepType(decl)
    }

    fun addVisibleStepName(decl: StepDeclaration) {
        stepConfig.addVisibleStepName(decl)
    }

    fun addVariable(binding: VariableBindingContainer) {
        stepConfig.addVariable(binding)
    }

    fun stepDeclaration(name: QName): DeclareStepInstruction? {
        return stepConfig.stepDeclaration(name)
    }

    fun stepAvailable(name: QName): Boolean {
        return stepDeclaration(name) != null
    }

    protected fun updateStepConfig(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        for ((type, decl) in stepTypes) {
            addVisibleStepType(decl)
        }
        for ((_, decl) in stepNames) {
            addVisibleStepName(decl)
        }
        for ((_, binding) in bindings) {
            addVariable(binding)
        }
    }

    internal open fun findDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        updateStepConfig(stepTypes, stepNames, bindings)
        for (child in _children) {
            child.findDeclarations(stepTypes, stepNames, bindings)
        }
    }

    internal open fun findDefaultReadablePort(drp: PortBindingContainer?) {
        stepConfig.drp = drp
        for (child in _children) {
            child.findDefaultReadablePort(drp)
        }
    }

    internal open fun elaborateInstructions() {
        for (child in _children) {
            child.elaborateInstructions()
        }
        open = false
    }

    internal fun hasAncestor(instruction: XProcInstruction): Boolean {
        var p: XProcInstruction? = this
        while (p != null) {
            if (p === instruction) {
                return true
            }
            p = p.parent
        }
        return false
    }

    internal fun findChild(seek: XProcInstruction): Int {
        for (index in children.indices) {
            if (children[index] === seek) {
                return index
            }
        }
        return -1
    }

    protected fun findStepDeclaration(stepType: QName): DeclareStepInstruction? {
        // You can't assume that it'll be in *this* stepConfig because imports
        // (on ancestor p:declare-step elements) may be updated after this
        // object has been constructed.
        var p: XProcInstruction? = this
        while (p != null) {
            if (p.stepConfig.stepDeclaration(stepType) != null) {
                return p.stepConfig.stepDeclaration(stepType)
            }
            p = p.parent
        }
        return null
    }

    fun fromString(xml: String, properties: DocumentProperties = DocumentProperties()): XProcDocument {
        return stepConfig.fromString(xml, properties, emptyMap())
    }

    override fun toString(): String {
        return "${instructionType}/${id}"
    }
}