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
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.ma.arrays.ArrayItemType
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.om.*
import net.sf.saxon.s9api.*
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.*
import java.net.URI

open class XProcStepConfigurationImpl internal constructor(
    override val environment: PipelineEnvironment,
    override val saxonConfig: SaxonConfiguration,
    location: Location
): XProcStepConfiguration, DocumentContext
{
    companion object {
        private val parsedXsTypes = mutableMapOf<String, SequenceType>()
        private var vara = QName("a")
        private var varb = QName("b")
    }

    protected var _location: Location = location
    override val location: Location
        get() = _location

    override val xmlCalabash: XmlCalabash
        get() = environment.xmlCalabash
    override val processor: Processor
        get() = saxonConfig.processor

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
        return copy
    }

    override fun copyNew(): XProcStepConfiguration {
        val copy = XProcStepConfigurationImpl(environment, saxonConfig.newConfiguration(), location)
        copy._inscopeNamespaces.putAll(_inscopeNamespaces)
        copy._inscopeStepTypes.putAll(_inscopeStepTypes)
        copy._stepName = _stepName
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
        val compiler = newXPathCompiler()
        val exec = compiler.compile(value)
        val selector = exec.load()
        val result = selector.evaluate()
        return validateAsType(result, type.underlyingSequenceType, inscopeNamespaces)
    }

    fun castAtomicAs(value: XdmAtomicValue, seqType: SequenceType?, inscopeNamespaces: Map<String, NamespaceUri>): XdmAtomicValue {
        if (seqType == null) {
            return value
        }
        return castAtomicAs(value, seqType.itemType, inscopeNamespaces)
    }

    fun castAtomicAs(value: XdmAtomicValue, xsdtype: ItemType, inscopeNamespaces: Map<String, NamespaceUri>): XdmAtomicValue {
        if (xsdtype == ItemType.UNTYPED_ATOMIC || xsdtype == ItemType.STRING || xsdtype == ItemType.ANY_ITEM) {
            return value
        }

        if (xsdtype == ItemType.QNAME) {
            val qname = when (value.primitiveTypeName) {
                NsXs.string, NsXs.untypedAtomic -> XdmAtomicValue(parseQName(value.stringValue, inscopeNamespaces))
                NsXs.QName -> value
                else -> throw exception(XProcError.xdBadType(value.stringValue, xsdtype))
            }
            return qname
        }

        try {
            return XdmAtomicValue(value.stringValue, xsdtype)
        } catch (ex: Exception) {
            when (ex) {
                is SaxonApiException -> {
                    if (ex.message!!.contains("Invalid URI")) {
                        throw exception(XProcError.xdInvalidUri(value.stringValue))
                    }
                    throw exception(XProcError.xdBadType(value.stringValue, xsdtype))

                }
                else -> throw ex
            }
        }
    }

    fun xpathInstanceOf(value: XdmValue, type: QName): Boolean {
        if (type.namespaceUri != NsXs.namespace) {
            throw exception(XProcError.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a instance of Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        return selector.effectiveBooleanValue()
    }

    fun xpathCastAs(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw exception(XProcError.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a cast as Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        return selector.evaluate()
    }

    fun xpathTreatAs(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw exception(XProcError.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a treat as Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        try {
            return selector.evaluate()
        } catch (ex: Exception) {
            throw exception(XProcError.xdBadType(ex.message ?: ""), ex)
        }
    }

    fun xpathPromote(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw exception(XProcError.xiImpossible("Attempt to cast to non-xs type"))
        }
        if (value.underlyingValue !is AtomicValue) {
            throw exception(XProcError.xiImpossible("Attempt to promote non-atomic: ${value}"))
        }

        val curType = (value.underlyingValue as AtomicValue).primitiveType

        if (curType == BuiltInAtomicType.ANY_ATOMIC || curType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return xpathCastAs(value, type)
        }

        if (curType.isNumericType && (type in NsXs.numericTypes)) {
            return xpathCastAs(value, type)
        }

        if (curType == BuiltInAtomicType.ANY_URI && type == NsXs.string) {
            return xpathCastAs(value, type)
        }

        if (curType == BuiltInAtomicType.STRING && (type == NsXs.anyURI || type == NsXs.NCName)) {
            return xpathCastAs(value, type)
        }

        return xpathTreatAs(value, type)
    }

    override fun checkType(
        varName: QName?,
        value: XdmValue,
        sequenceType: SequenceType?,
        values: List<XdmAtomicValue>
    ): XdmValue {
        return checkType(varName, value, sequenceType, inscopeNamespaces, values)
    }

    override fun checkType(
        varName: QName?,
        value: XdmValue,
        sequenceType: SequenceType?,
        inscopeNamespaces: Map<String, NamespaceUri>,
        values: List<XdmAtomicValue>
    ): XdmValue {
        var newValue = value
        if (sequenceType != null) {
            val code = sequenceType.itemType.underlyingItemType.basicAlphaCode

            if (value === XdmEmptySequence.getInstance()) {
                if (sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO
                    || sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO_OR_ONE
                    || sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO_OR_MORE) {
                    return value
                }
                throw exception(XProcError.xdBadType("Empty sequence not allowed"))
                /*
                if (code == "AB") {
                    return XdmAtomicValue(false)
                }
                if (varName == null) {
                    throw exception(XProcError.xdBadType(value.underlyingValue.stringValue, sequenceType.underlyingSequenceType.toString()))
                } else {
                    throw exception(XProcError.xdBadType(varName, value.toString(), sequenceType.underlyingSequenceType.toString()))
                }
                 */
            }

            // We've got a bit of a hack here. If the input is a string, but the type is a
            // map or array, attempt to parse the string. For simple atomic values, like strings
            // or numbers, we don't have to do this
            newValue = if (value.underlyingValue is StringValue && (code == "FA" || code == "FM")) {
                parseAsType(value.underlyingValue.stringValue, sequenceType, inscopeNamespaces)
            } else {
                validateAsType(value, sequenceType.underlyingSequenceType, inscopeNamespaces)
            }
        }

        if (values.isNotEmpty()) {
            for (item in newValue) {
                checkType(item, values)
            }
        }

        return newValue
    }

    private fun checkType(value: XdmValue, values: List<XdmAtomicValue>) {
        for (alt in values) {
            if (xpathEq(value, alt)) {
                return
            }
        }
        throw exception(XProcError.xdValueNotAllowed(value, values))
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
        return forceQNameKeys(inputMap, inscopeNamespaces)
    }

    override fun forceQNameKeys(
        inputMap: XdmMap,
        inscopeNamespaces: Map<String, NamespaceUri>
    ): XdmMap {
        var map = XdmMap()
        for (key in inputMap.keySet()) {
            val value = inputMap.get(key)
            val mapkey = when (key.underlyingValue.itemType) {
                BuiltInAtomicType.STRING -> parseQName(key.stringValue, inscopeNamespaces)
                BuiltInAtomicType.QNAME -> {
                    val qvalue = key.underlyingValue as QNameValue
                    QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
                }

                else -> throw RuntimeException("key isn't string or qname?")
            }

            if (mapkey == Ns.serialization) {
                if (value is XdmMap) {
                    try {
                        map = map.put(XdmAtomicValue(mapkey), forceQNameKeys(value, inscopeNamespaces))
                    } catch (ex: XProcException) {
                        if (ex.error.code == NsErr.xd(36)) {
                            val name = ex.error.details.getOrNull(0)
                            if (name is String) {
                                exception(XProcError.xdInvalidSerialization(name))
                            }
                            throw exception(XProcError.xdInvalidSerialization())
                        }
                    }
                } else {
                    throw exception(XProcError.xdInvalidSerialization())
                }
            } else {
                map = map.put(XdmAtomicValue(mapkey), value)
            }
        }
        return map
    }

    override fun exception(error: XProcError): XProcException {
        // TODO: add more context to the error
        return error.at(location).exception()
    }

    override fun exception(error: XProcError, cause: Throwable): XProcException {
        return XProcException(error.at(location), cause)
    }

    override fun parseBoolean(bool: String): Boolean {
        val value = castAtomicAs(XdmAtomicValue(bool), ItemType.BOOLEAN, mapOf())
        return value.booleanValue
    }

    override fun parseQName(name: String): QName {
        return parseQName(name, inscopeNamespaces, NamespaceUri.NULL)
    }

    override fun parseQName(
        name: String,
        inscopeNamespaces: Map<String, NamespaceUri>
    ): QName {
        return parseQName(name, inscopeNamespaces, NamespaceUri.NULL)
    }

    override fun parseQName(
        name: String,
        inscopeNamespaces: Map<String, NamespaceUri>,
        defaultNamespace: NamespaceUri
    ): QName {
        if (name.startsWith("Q{")) {
            val pos = name.indexOf("}")
            if (pos < 0) {
                throw exception(XProcError.xdInvalidQName(name))
            }
            return QName(NamespaceUri.of(name.substring(2, pos)), parseNCName(name.substring(pos+1)))
        }

        val pos = name.indexOf(":")
        if (pos < 0) {
            return QName(defaultNamespace, parseNCName(name))
        }

        val prefix = parseNCName(name.substring(0, pos))
        if (inscopeNamespaces.containsKey(prefix)) {
            parseNCName(name.substring(pos+1)) // check that the local name is an NCName
            return QName(inscopeNamespaces[prefix], name)
        } else {
            throw exception(XProcError.xdInvalidPrefix(name, prefix))
        }
    }

    override fun parseNCName(name: String): String {
        val value = castAtomicAs(XdmAtomicValue(name), ItemType.NCNAME, mapOf())
        return value.stringValue
    }

    override fun stringAttributeMap(attr: Map<String, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(QName(name), value))
            }
        }
        return map
    }

    override fun attributeMap(attr: Map<QName, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(name, value))
            }
        }
        return map
    }

    override fun attributeMap(attributes: AttributeMap): Map<QName, String?> {
        val map = mutableMapOf<QName, String?>()
        for (attr in attributes) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.uri, attr.nodeName.localPart)
            map[qname] = attr.value
        }
        return map
    }

    override fun asXdmMap(inputMap: Map<QName, XdmValue>): XdmMap {
        var map = XdmMap()
        for ((key, value) in inputMap) {
            map = map.put(XdmAtomicValue(key), value)
        }
        return map
    }

    override fun asMap(inputMap: XdmMap): Map<QName, XdmValue> {
        val map = mutableMapOf<QName, XdmValue>()
        for (key in inputMap.keySet()) {
            val value = inputMap.get(key)
            val qvalue = key.underlyingValue
            val qkey = if (qvalue is QNameValue) {
                QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
            } else {
                throw RuntimeException("Expected map of QName keys")
            }
            map.put(qkey, value)
        }
        return map
    }

    override fun parseXsSequenceType(asExpr: String): SequenceType {
        parsedXsTypes[asExpr]?.let { return it }

        val config = processor.getUnderlyingConfiguration()
        val icontext = IndependentContext(config)
        icontext.clearAllNamespaces() // We get no defaults
        icontext.declareNamespace("xs", NsXs.namespace)
        icontext.declareNamespace("fn", NsFn.namespace)
        icontext.declareNamespace("map", NsFn.mapNamespace)
        icontext.declareNamespace("array", NsFn.arrayNamespace)
        icontext.declareNamespace("math", NsFn.mathNamespace)
        icontext.declareNamespace("saxon", NamespaceUri.of("http://saxon.sf.net/"))

        icontext.setXPathLanguageLevel(31)
        val parser = XPathParser(icontext)
        val st = parser.parseSequenceType(asExpr, icontext)
        parsedXsTypes[asExpr] = SequenceType.fromUnderlyingSequenceType(processor, st)
        return parsedXsTypes[asExpr]!!
    }

    override fun xpathEq(left: XdmValue, right: XdmValue): Boolean {
        return xpathEqual(left, right, "eq")
    }

    override fun xpathEqual(left: XdmValue, right: XdmValue): Boolean {
        return xpathEqual(left, right, "=")
    }

    private fun xpathEqual(left: XdmValue, right: XdmValue, op: String): Boolean {
        val compiler = newXPathCompiler()
        compiler.declareVariable(vara)
        compiler.declareVariable(varb)
        val exec = compiler.compile("\$a ${op} \$b")
        val selector = exec.load()
        selector.setVariable(vara, left)
        selector.setVariable(varb, right)
        return selector.effectiveBooleanValue()
    }

    override fun xpathDeepEqual(left: XdmValue, right: XdmValue): Boolean {
        val compiler = newXPathCompiler()
        compiler.declareVariable(vara)
        compiler.declareVariable(varb)
        val exec = compiler.compile("deep-equal(\$a,\$b)")
        val selector = exec.load()
        selector.setVariable(vara, left)
        selector.setVariable(varb, right)
        return selector.effectiveBooleanValue()
    }

    override fun resolve(href: String): URI {
        if (baseUri == null) {
            return URI(href)
        }
        return baseUri!!.resolve(href)
    }

    // ============================================================

    private fun attributeInfo(name: QName, value: String, location: net.sf.saxon.s9api.Location? = null): AttributeInfo {
        return AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
    }

    private fun fqName(name: QName): FingerprintedQName = FingerprintedQName(name.prefix, name.namespaceUri, name.localName)

    fun validateAsType(value: XdmValue, type: net.sf.saxon.value.SequenceType, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        if (type.primaryType is ArrayItemType) {
            return validateAsArray(value as XdmArray, type, inscopeNamespaces)
        }
        if (type.primaryType is MapType) {
            return validateAsMap(value as XdmMap, type, inscopeNamespaces)
        }

        val values = mutableListOf<XdmValue>()
        val code = type.primaryType.basicAlphaCode
        for (item in value) {
            val itemValue = checkSimpleType(item, code, inscopeNamespaces)
            values.add(itemValue)
        }

        if (values.size == 1) {
            return values[0]
        }

        var newValue: XdmValue = XdmEmptySequence.getInstance()
        for (item in values) {
            newValue = newValue.append(item)
        }
        return newValue
    }

    private fun validateAsArray(value: XdmArray, type: net.sf.saxon.value.SequenceType, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        val memberType = (type.primaryType as ArrayItemType).memberType
        val memberList = mutableListOf<XdmValue>()
        for (index in 0..< value.arrayLength()) {
            val member = value[index]
            memberList.add(validateAsType(member, memberType!!, inscopeNamespaces))
        }
        return XdmArray(memberList.toTypedArray())
    }

    private fun validateAsMap(value: XdmMap, type: net.sf.saxon.value.SequenceType, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        val keyType = (type.primaryType as MapType).keyType
        val valueType = (type.primaryType as MapType).valueType
        var newValue = XdmMap()
        for (key in value.keySet()) {
            val mvalue = value.get(key)
            val keyValue = checkSimpleType(key, keyType.basicAlphaCode, inscopeNamespaces)
            val memberValue = validateAsType(mvalue, valueType, inscopeNamespaces)
            newValue = newValue.put(keyValue as XdmAtomicValue, memberValue)
        }
        return newValue
    }

    private fun checkSimpleType(value: XdmValue, code: String, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        if (value !is XdmAtomicValue) {
            return value
        }

        var typeName = NsXs.anyAtomicType

        when (code) {
            "", "A" -> return value
            "AB" -> {
                if (value.underlyingValue is BooleanValue) {
                    return value
                }
                typeName = NsXs.boolean
            }

            "AS" -> {
                if (value.underlyingValue is StringValue) {
                    return value
                }
                typeName = NsXs.string
            }
            "ASN" -> {
                typeName = NsXs.normalizedString
            }
            "ASNT" -> {
                typeName = NsXs.token
            }
            "ASNTL" -> {
                typeName = NsXs.language
            }
            "ASNTK" -> {
                typeName = NsXs.NMTOKEN
            }
            "ASNTN" -> {
                typeName = NsXs.name
            }
            "ASNTNC" -> {
                typeName = NsXs.NCName
            }
            "ASNTNCI" -> {
                typeName = NsXs.ID
            }
            "ASNTNCE" -> {
                typeName = NsXs.ENTITY
            }
            "ASNTNCR" -> {
                typeName = NsXs.IDREF
            }
            "AQ" -> {
                if (value.underlyingValue is QNameValue) {
                    return value
                }
                return XdmAtomicValue(parseQName(value.stringValue, inscopeNamespaces))
            }

            "AU" -> {
                if (value.underlyingValue is AnyURIValue) {
                    return value
                }
                typeName = NsXs.anyURI
            }

            "AA" -> {
                if (value.underlyingValue is DateValue) {
                    return value
                }
                typeName = NsXs.date
            }

            "AM" -> {
                if (value.underlyingValue is DateTimeValue) {
                    return value
                }
                typeName = NsXs.dateTime
            }

            "AMP" -> {
                if (value.underlyingValue is DateTimeValue) {
                    return value
                }
                typeName = NsXs.dateTimeStamp
            }

            "AT" -> {
                if (value.underlyingValue is TimeValue) {
                    return value
                }
                typeName = NsXs.time
            }

            "AR" -> {
                if (value.underlyingValue is DurationValue) {
                    return value
                }
                typeName = NsXs.duration
            }

            "ARD" -> {
                if (value.underlyingValue is DayTimeDurationValue) {
                    return value
                }
                typeName = NsXs.dayTimeDuration
            }

            "ARY" -> {
                if (value.underlyingValue is YearMonthDurationValue) {
                    return value
                }
                typeName = NsXs.yearMonthDuration
            }

            "AG" -> {
                if (value.underlyingValue is GYearValue) {
                    return value
                }
                typeName = NsXs.gYear
            }

            "AH" -> {
                if (value.underlyingValue is GYearMonthValue) {
                    return value
                }
                typeName = NsXs.gYearMonth
            }

            "AI" -> {
                if (value.underlyingValue is GMonthValue) {
                    return value
                }
                typeName = NsXs.gMonth
            }

            "AJ" -> {
                if (value.underlyingValue is GMonthDayValue) {
                    return value
                }
                typeName = NsXs.gMonthDay
            }

            "AK" -> {
                if (value.underlyingValue is GDayValue) {
                    return value
                }
                typeName = NsXs.gDay
            }

            "AD" -> {
                if (value.underlyingValue is DecimalValue) {
                    return value
                }
                typeName = NsXs.decimal
            }

            "ADI" -> {
                if (value.underlyingValue is Int64Value) {
                    return value
                }
                typeName = NsXs.decimal
            }

            "ADIN" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() <= 0) {
                    return value
                }
                typeName = NsXs.nonPositiveInteger
            }

            "ADINN" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() < 0) {
                    return value
                }
                typeName = NsXs.negativeInteger
            }

            "ADIP" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= 0) {
                    return value
                }
                typeName = NsXs.nonNegativeInteger
            }

            "ADIPP" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() > 0) {
                    return value
                }
                typeName = NsXs.positiveInteger
            }

            "ADIPL" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= 0) {
                    return value
                }
                typeName = NsXs.unsignedLong
            }

            "ADIPLI" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= 0 && uvalue.longValue() < 65536) {
                    return value
                }
                typeName = NsXs.unsignedInt
            }

            "ADIPLIS" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= 0 && uvalue.longValue() < 32768) {
                    return value
                }
                typeName = NsXs.unsignedShort
            }

            "ADIPLISB" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= 0 && uvalue.longValue() < 256) {
                    return value
                }
                typeName = NsXs.unsignedByte
            }

            "ADIL" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value) {
                    return value
                }
                typeName = NsXs.long
            }

            "ADILI" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= -65536 && uvalue.longValue() < 65536) {
                    return value
                }
                typeName = NsXs.int
            }

            "ADILIS" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= -32768 && uvalue.longValue() < 32768) {
                    return value
                }
                typeName = NsXs.short
            }

            "ADILISB" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Int64Value && uvalue.longValue() >= -128 && uvalue.longValue() < 128) {
                    return value
                }
                typeName = NsXs.byte
            }

            "AO" -> {
                val uvalue = value.underlyingValue
                if (uvalue is DoubleValue) {
                    return value
                }
                typeName = NsXs.double
            }

            "AF" -> {
                val uvalue = value.underlyingValue
                if (uvalue is FloatValue) {
                    return value
                }
                typeName = NsXs.float
            }

            "A2" -> {
                val uvalue = value.underlyingValue
                if (uvalue is Base64BinaryValue) {
                    return value
                }
                typeName = NsXs.base64Binary
            }

            "AX" -> {
                val uvalue = value.underlyingValue
                if (uvalue is HexBinaryValue) {
                    return value
                }
                typeName = NsXs.hexBinary
            }

            "AZ" -> {
                typeName = NsXs.untypedAtomic
            }
        }

        //val itemType = itemTypeFactory.getAtomicType(typeName)
        return xpathPromote(value, typeName)
    }

}