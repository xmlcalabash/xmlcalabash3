package com.xmlcalabash.runtime

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.PipelineEnvironment
import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*

interface XProcStepConfiguration: DocumentContext {
    val environment: PipelineEnvironment
    val saxonConfig: SaxonConfiguration
    var validationMode: ValidationMode

    val inscopeStepTypes: Map<QName, DeclareStepInstruction>

    var stepName: String
    val location: Location
    val nextId: String

    fun error(message: () -> String)
    fun warn(message: () -> String)
    fun info(message: () -> String)
    fun debug(message: () -> String)
    fun trace(message: () -> String)

    fun copy(): XProcStepConfiguration
    fun copyNew(): XProcStepConfiguration
    fun copy(config: XProcStepConfiguration): XProcStepConfiguration

    fun putNamespace(prefix: String, uri: NamespaceUri)
    fun putAllNamespaces(namespaces: Map<String, NamespaceUri>)
    fun putStepType(name: QName, declare: DeclareStepInstruction)
    fun putAllStepTypes(types: Map<QName, DeclareStepInstruction>)
    fun setLocation(location: Location)

    fun parseBoolean(bool: String): Boolean
    fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>): QName
    fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>, defaultNamespace: NamespaceUri): QName
    fun stringAttributeMap(attr: Map<String,String?>): AttributeMap
    fun attributeMap(attr: Map<QName,String?>): AttributeMap
    fun attributeMap(attributes: AttributeMap): Map<QName,String?>
    fun asXdmMap(inputMap: Map<QName, XdmValue>): XdmMap
    fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, inscopeNamespaces: Map<String, NamespaceUri>, values: List<XdmAtomicValue>): XdmValue
    fun asMap(inputMap: XdmMap): Map<QName, XdmValue>
    fun parseXsSequenceType(asExpr: String): SequenceType
    fun xpathEq(left: XdmValue, right: XdmValue): Boolean
    fun xpathEqual(left: XdmValue, right: XdmValue): Boolean
    fun xpathDeepEqual(left: XdmValue, right: XdmValue): Boolean

    fun stepDeclaration(name: QName): DeclareStepInstruction?
    fun stepAvailable(name: QName): Boolean
    fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, values: List<XdmAtomicValue>): XdmValue
    fun forceQNameKeys(inputMap: MapItem): XdmMap
    fun forceQNameKeys(inputMap: XdmMap): XdmMap
    fun exception(error: XProcError): XProcException
    fun exception(error: XProcError, cause: Throwable): XProcException
}