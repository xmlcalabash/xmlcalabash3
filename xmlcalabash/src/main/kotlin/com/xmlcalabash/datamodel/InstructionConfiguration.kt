package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import java.net.URI

interface InstructionConfiguration: XProcStepConfiguration {
    val inscopeStepNames: Map<String, StepDeclaration>
    val inscopeVariables: Map<QName, VariableBindingContainer>
    val staticBindings: Map<QName, XdmValue>
    val qnameMapType: SequenceType

    var drp: PortBindingContainer?

    override fun copy(): InstructionConfiguration
    override fun copyNew(): InstructionConfiguration

    fun addVisibleStepName(decl: StepDeclaration)
    fun addVisibleStepType(decl: DeclareStepInstruction)
    fun addVariable(binding: VariableBindingContainer)
    fun addStaticBinding(name: QName, value: XdmValue)

    fun with(location: Location): InstructionConfiguration
    fun with(prefix: String, uri: NamespaceUri): InstructionConfiguration
    fun with(namespaces: Map<String, NamespaceUri>): InstructionConfiguration

    fun updateWith(node: XdmNode)
    fun updateWith(baseUri: URI)

    fun fromUri(href: URI, properties: DocumentProperties, parameters: Map<QName, XdmValue>): XProcDocument
    fun fromString(xml: String, properties: DocumentProperties, parameters: Map<QName, XdmValue>): XProcDocument

    fun parseVisibility(visible: String): Visibility
    fun parseContentTypes(text: String): List<MediaType>
    fun parseExcludeInlinePrefixes(prefixes: String): Set<NamespaceUri>
    fun parseValues(text: String): List<XdmAtomicValue>
    fun parseSequenceType(asExpr: String): SequenceType
    fun parseSpecialType(type: String): SpecialType
}