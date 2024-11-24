package com.xmlcalabash.runtime

import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*

interface ValueConverter {
    fun parseBoolean(bool: String): Boolean
    fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>): QName
    fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>, defaultNamespace: NamespaceUri): QName
    fun forceQNameKeys(inputMap: MapItem, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
    fun forceQNameKeys(inputMap: XdmMap, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap
    fun parseNCName(name: String): String
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
}