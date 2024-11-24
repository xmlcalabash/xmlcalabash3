package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.net.URI

abstract class XProcInstruction internal constructor(initialParent: XProcInstruction?, val stepConfig: StepConfiguration, val instructionType: QName) {
    internal lateinit var builder: PipelineBuilder
    internal var _parent = initialParent
    val parent: XProcInstruction?
        get() = _parent

    init {
        if (initialParent != null) {
            builder = initialParent.builder
        }
    }

    val id = stepConfig.nextId
    var expandText: Boolean? = null

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

    internal val _pipeInfo = mutableListOf<XdmNode>()
    val pipeInfo: List<XdmNode>
        get() = _pipeInfo

    internal var open = true
    protected fun checkOpen() {
        if (!open) {
            throw IllegalArgumentException("${instructionType} cannot be changed")
        }
    }

    val inscopeNamespaces: Map<String, NamespaceUri>
        get() = stepConfig.inscopeNamespaces

    val inscopeStepNames: Map<String, StepDeclaration>
        get() = stepConfig.inscopeStepNames

    val inscopeStepTypes: Map<QName, DeclareStepInstruction>
        get() = stepConfig.inscopeStepTypes

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

    internal fun addRewriteEmpty(step: StepDeclaration): AtomicStepInstruction {
        val emptyStep = AtomicStepInstruction(step, NsCx.empty)
        emptyStep.elaborateAtomicStep()
        return emptyStep
    }

    fun fromUri(href: URI, properties: DocumentProperties = DocumentProperties(), parameters: Map<QName, XdmValue> = emptyMap()): XProcDocument {
        return stepConfig.fromUri(href, properties, parameters)
    }

    fun fromString(xml: String, properties: DocumentProperties = DocumentProperties()): XProcDocument {
        return stepConfig.fromString(xml, properties)
    }

    override fun toString(): String {
        return "${instructionType}/${id}"
    }
}