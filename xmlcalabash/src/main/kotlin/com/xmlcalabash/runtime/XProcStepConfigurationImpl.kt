package com.xmlcalabash.runtime

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.PipelineEnvironment
import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.TypeUtils
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.QNameValue
import java.net.URI

open class XProcStepConfigurationImpl internal constructor(
    override val environment: PipelineEnvironment,
    override val saxonConfig: SaxonConfiguration,
    location: Location
): XProcStepConfiguration, DocumentContext {
    protected var _location: Location = location
    override val location: Location
        get() = _location

    override val xmlCalabash: XmlCalabash
        get() = environment.xmlCalabash
    override val processor: Processor
        get() = saxonConfig.processor

    override var validationMode = ValidationMode.DEFAULT

    protected val _inscopeNamespaces = mutableMapOf<String, NamespaceUri>()
    protected val _inscopeStepTypes = mutableMapOf<QName, DeclareStepInstruction>()

    override val inscopeNamespaces: Map<String, NamespaceUri>
        get() = _inscopeNamespaces
    override val inscopeStepTypes: Map<QName, DeclareStepInstruction>
        get() = _inscopeStepTypes

    private var _stepName: String? = null
    override var stepName: String
        get() = _stepName!!
        set(value) {
            _stepName = value
        }

    override val baseUri: URI?
        get() = _location.baseUri

    private var _nextId: String? = null
    override val nextId: String
        get() {
            if (_nextId == null) {
                _nextId = environment.nextId
            }
            return _nextId!!
        }

    private val typeUtils = TypeUtils(this)

    override fun error(message: () -> String) {
        report(Verbosity.ERROR, emptyMap(), message)
    }

    override fun warn(message: () -> String) {
        report(Verbosity.WARN, emptyMap(), message)
    }

    override fun info(message: () -> String) {
        report(Verbosity.INFO, emptyMap(), message)
    }

    override fun debug(message: () -> String) {
        report(Verbosity.DEBUG, emptyMap(), message)
    }

    override fun trace(message: () -> String) {
        report(Verbosity.TRACE, emptyMap(), message)
    }

    fun report(verbosity: Verbosity, extraAttributes: Map<QName, String>, message: () -> String) {
        val extra = mutableMapOf<QName, String>()
        extra.putAll(extraAttributes)

        if (_stepName != null && !stepName.startsWith("!")) {
            extra[Ns.stepName] = stepName
        }
        baseUri?.let { extra[Ns.baseUri] = "${it}" }
        if (location.lineNumber > 0) {
            extra[Ns.lineNumber] = "${location.lineNumber}"
        }
        if (location.columnNumber > 0) {
            extra[Ns.columnNumber] = "${location.columnNumber}"
        }
        environment.messageReporter.report(verbosity, extra, message)
    }

    override fun copy(): XProcStepConfiguration {
        val copy = XProcStepConfigurationImpl(environment, saxonConfig, location)
        copy._inscopeNamespaces.putAll(_inscopeNamespaces)
        copy._inscopeStepTypes.putAll(_inscopeStepTypes)
        copy._stepName = _stepName
        copy.validationMode = validationMode
        return copy
    }

    override fun copyNew(): XProcStepConfiguration {
        val copy = XProcStepConfigurationImpl(environment, saxonConfig.newConfiguration(), location)
        copy._inscopeNamespaces.putAll(_inscopeNamespaces)
        copy._inscopeStepTypes.putAll(_inscopeStepTypes)
        copy._stepName = _stepName
        copy.validationMode = validationMode
        return copy
    }

    override fun copy(config: XProcStepConfiguration): XProcStepConfiguration {
        return config.copy()
    }

    override fun putNamespace(prefix: String, uri: NamespaceUri) {
        _inscopeNamespaces[prefix] = uri
    }

    override fun putAllNamespaces(namespaces: Map<String, NamespaceUri>) {
        _inscopeNamespaces.putAll(namespaces)
    }

    override fun putStepType(type: QName, decl: DeclareStepInstruction) {
        _inscopeStepTypes[type] = decl
    }

    override fun putAllStepTypes(types: Map<QName, DeclareStepInstruction>) {
        _inscopeStepTypes.putAll(types)
    }

    override fun setLocation(location: Location) {
        _location = location
    }

    override fun newXPathCompiler(): XPathCompiler {
        val compiler = processor.newXPathCompiler()
        compiler.baseURI = baseUri
        compiler.isSchemaAware = processor.isSchemaAware
        for ((prefix, value) in inscopeNamespaces) {
            compiler.declareNamespace(prefix, value.toString())
        }
        return compiler
    }

    override fun stepDeclaration(name: QName): DeclareStepInstruction? {
        return _inscopeStepTypes[name] ?: environment.standardSteps[name]
    }

    override fun stepAvailable(name: QName): Boolean {
        val decl = stepDeclaration(name)
        if (decl != null) {
            return !decl.isAtomic || environment.commonEnvironment.atomicStepAvailable(name)
        }
        return false
    }

    fun parseAsType(value: String, type: SequenceType, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        return typeUtils.parseAsType(value, type, inscopeNamespaces)
    }

    fun castAtomicAs(value: XdmAtomicValue, seqType: SequenceType?, inscopeNamespaces: Map<String, NamespaceUri>): XdmAtomicValue {
        return typeUtils.castAtomicAs(value, seqType, inscopeNamespaces)
    }

    fun castAtomicAs(value: XdmAtomicValue, xsdtype: ItemType, inscopeNamespaces: Map<String, NamespaceUri>): XdmAtomicValue {
        return typeUtils.castAtomicAs(value, xsdtype, inscopeNamespaces)
    }

    fun xpathInstanceOf(value: XdmValue, type: QName): Boolean {
        return typeUtils.xpathInstanceOf(value, type)
    }

    fun xpathCastAs(value: XdmValue, type: QName): XdmValue {
        return typeUtils.xpathCastAs(value, type)
    }

    fun xpathTreatAs(value: XdmValue, type: QName): XdmValue {
        return typeUtils.xpathTreatAs(value, type)
    }

    fun xpathPromote(value: XdmValue, type: QName): XdmValue {
        return typeUtils.xpathPromote(value, type)
    }

    override fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, values: List<XdmAtomicValue>): XdmValue {
        return typeUtils.checkType(varName, value, sequenceType, values)
    }

    override fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, inscopeNamespaces: Map<String, NamespaceUri>, values: List<XdmAtomicValue>): XdmValue {
        return typeUtils.checkType(varName, value, sequenceType, inscopeNamespaces, values)
    }

    override fun forceQNameKeys(inputMap: MapItem): XdmMap {
        return forceQNameKeys(inputMap, inscopeNamespaces)
    }

    override fun forceQNameKeys(
        inputMap: MapItem,
        inscopeNamespaces: Map<String, NamespaceUri>
    ): XdmMap {
        var map = XdmMap()
        for (pair in inputMap.keyValuePairs()) {
            when (pair.key.itemType) {
                BuiltInAtomicType.STRING -> {
                    val qname = parseQName(pair.key.stringValue, inscopeNamespaces)
                    map = map.put(XdmAtomicValue(qname), XdmValue.wrap(pair.value))
                }

                BuiltInAtomicType.QNAME -> {
                    val qvalue = pair.key as QNameValue
                    val key = QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
                    map = map.put(XdmAtomicValue(key), XdmValue.wrap(pair.value))
                }

                else -> {
                    throw RuntimeException("key isn't string or qname?")
                }
            }
        }
        return map
    }

    override fun forceQNameKeys(inputMap: XdmMap): XdmMap {
        return typeUtils.forceQNameKeys(inputMap)
    }

    override fun forceQNameKeys(inputMap: XdmMap, inscopeNamespaces: Map<String, NamespaceUri>): XdmMap {
        return typeUtils.forceQNameKeys(inputMap, inscopeNamespaces)
    }

    override fun exception(error: XProcError): XProcException {
        // TODO: add more context to the error
        return error.at(location).exception()
    }

    override fun exception(error: XProcError, cause: Throwable): XProcException {
        return XProcException(error.at(location), cause)
    }

    override fun parseBoolean(bool: String): Boolean {
        return typeUtils.parseBoolean(bool)
    }

    override fun parseQName(name: String): QName {
        return typeUtils.parseQName(name)
    }

    override fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>): QName {
        return typeUtils.parseQName(name, inscopeNamespaces)
    }

    override fun parseQName(name: String, inscopeNamespaces: Map<String, NamespaceUri>, defaultNamespace: NamespaceUri): QName {
        return typeUtils.parseQName(name, inscopeNamespaces, defaultNamespace)
    }

    override fun parseNCName(name: String): String {
        return typeUtils.parseNCName(name)
    }

    override fun stringAttributeMap(attr: Map<String, String?>): AttributeMap {
        return typeUtils.stringAttributeMap(attr)
    }

    override fun attributeMap(attr: Map<QName, String?>): AttributeMap {
        return typeUtils.attributeMap(attr)
    }

    override fun attributeMap(attributes: AttributeMap): Map<QName, String?> {
        return typeUtils.attributeMap(attributes)
    }

    override fun asXdmMap(inputMap: Map<QName, XdmValue>): XdmMap {
        return typeUtils.asXdmMap(inputMap)
    }

    override fun asMap(inputMap: XdmMap): Map<QName, XdmValue> {
        return typeUtils.asMap(inputMap)
    }

    override fun parseXsSequenceType(asExpr: String): SequenceType {
        return typeUtils.parseXsSequenceType(asExpr)
    }

    override fun xpathEq(left: XdmValue, right: XdmValue): Boolean {
        return typeUtils.xpathEq(left, right)
    }

    override fun xpathEqual(left: XdmValue, right: XdmValue): Boolean {
        return typeUtils.xpathEqual(left, right)
    }

    override fun xpathDeepEqual(left: XdmValue, right: XdmValue): Boolean {
        return typeUtils.xpathDeepEqual(left, right)
    }

    override fun resolve(href: String): URI {
        if (baseUri == null) {
            return URI(href)
        }
        return baseUri!!.resolve(href)
    }
}