package com.xmlcalabash.util

import com.xmlcalabash.datamodel.DocumentContext
import com.xmlcalabash.datamodel.SpecialType
import com.xmlcalabash.datamodel.Visibility
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.ma.arrays.ArrayItem
import net.sf.saxon.ma.arrays.ArrayItemType
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.om.GroundedValue
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.Location
import net.sf.saxon.s9api.OccurrenceIndicator
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.AnyURIValue
import net.sf.saxon.value.AtomicValue
import net.sf.saxon.value.Base64BinaryValue
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.DateTimeValue
import net.sf.saxon.value.DateValue
import net.sf.saxon.value.DayTimeDurationValue
import net.sf.saxon.value.DecimalValue
import net.sf.saxon.value.DoubleValue
import net.sf.saxon.value.DurationValue
import net.sf.saxon.value.FloatValue
import net.sf.saxon.value.GDayValue
import net.sf.saxon.value.GMonthDayValue
import net.sf.saxon.value.GMonthValue
import net.sf.saxon.value.GYearMonthValue
import net.sf.saxon.value.GYearValue
import net.sf.saxon.value.HexBinaryValue
import net.sf.saxon.value.Int64Value
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.StringValue
import net.sf.saxon.value.TimeValue
import net.sf.saxon.value.YearMonthDurationValue
import kotlin.collections.iterator

class TypeUtils(val context: DocumentContext) {
    companion object {
        private val parsedXsTypes = mutableMapOf<String, SequenceType>()
        private var vara = QName("a")
        private var varb = QName("b")

        fun sequenceTypeToString(asType: SequenceType): String {
            val baseType = asType.itemType.typeName
            val occur = asType.occurrenceIndicator.toString()
            return "${baseType}${occur}"
        }
    }

    fun parseBoolean(bool: String): Boolean {
        val value = castAtomicAs(XdmAtomicValue(bool), ItemType.BOOLEAN, mapOf())
        return value.booleanValue
    }

    fun parseQName(name: String): QName {
        return parseQName(name, context.inscopeNamespaces, NamespaceUri.NULL)
    }

    fun parseQName(
        name: String,
        inscopeNamespaces: Map<String, NamespaceUri>
    ): QName {
        return parseQName(name, inscopeNamespaces, NamespaceUri.NULL)
    }

    fun parseQName(
        name: String,
        inscopeNamespaces: Map<String, NamespaceUri>,
        defaultNamespace: NamespaceUri
    ): QName {
        if (name.startsWith("Q{")) {
            val pos = name.indexOf("}")
            if (pos < 0) {
                throw context.exception(XProcError.Companion.xdInvalidQName(name))
            }
            return QName(NamespaceUri.of(name.substring(2, pos)), parseNCName(name.substring(pos + 1)))
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
            throw context.exception(XProcError.Companion.xdInvalidPrefix(name, prefix))
        }
    }

    fun parseNCName(name: String): String {
        val value = castAtomicAs(XdmAtomicValue(name), ItemType.NCNAME, mapOf())
        return value.stringValue
    }

    fun stringAttributeMap(attr: Map<String, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(QName(name), value))
            }
        }
        return map
    }

    fun attributeMap(attr: Map<QName, String?>): AttributeMap {
        var map: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attr) {
            if (value != null) {
                map = map.put(attributeInfo(name, value))
            }
        }
        return map
    }

    fun attributeMap(attributes: AttributeMap): Map<QName, String?> {
        val map = mutableMapOf<QName, String?>()
        for (attr in attributes) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.uri, attr.nodeName.localPart)
            map[qname] = attr.value
        }
        return map
    }

    fun asXdmMap(inputMap: Map<QName, XdmValue>): XdmMap {
        var map = XdmMap()
        for ((key, value) in inputMap) {
            map = map.put(XdmAtomicValue(key), value)
        }
        return map
    }

    fun asXdmMap(inputMap: MapItem): XdmMap {
        var map = XdmMap()
        for (pair in inputMap.keyValuePairs()) {
            var value: XdmValue = XdmEmptySequence.getInstance()
            val key = XdmAtomicValue(pair.key)
            for (index in 0 ..< pair.value.length) {
                val item = pair.value.itemAt(index)
                when (item) {
                    is NodeInfo -> value = value.append(XdmNode(item))
                    is GroundedValue -> value = value.append(XdmValue.wrap(item))
                    else -> throw IllegalArgumentException("Unsupported item type in map conversion")
                }
            }
            map = map.put(key, value)
        }
        return map
    }

    fun asMap(inputMap: XdmMap): Map<QName, XdmValue> {
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

    fun asXdmArray(inputArray: ArrayItem): XdmArray {
        var array = XdmArray()
        for (index in 0 ..< inputArray.length) {
            val item = inputArray.itemAt(index)
            when (item) {
                is NodeInfo -> array = array.addMember(XdmNode(item))
                is GroundedValue -> array = array.addMember(XdmValue.wrap(item))
                else -> throw IllegalArgumentException("Unsupported item type in array conversion")
            }
        }
        return array
    }

    fun parseXsSequenceType(asExpr: String): SequenceType {
        parsedXsTypes[asExpr]?.let { return it }

        val processor = context.processor
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

    fun parseSpecialType(type: String): SpecialType {
        when (type) {
            "XPathExpression" -> return SpecialType.XPATH_EXPRESSION
            "XSLTSelectionPattern" -> return SpecialType.XSLT_SELECTION_PATTERN
            "RegularExpression" -> return SpecialType.REGULAR_EXPRESSION
            "EQNameList" -> return SpecialType.EQNAME_LIST
            else -> {
                throw context.exception(XProcError.Companion.xiNotASpecialType(type))
            }
        }
    }

    fun parseExcludeInlinePrefixes(prefixes: String): Set<NamespaceUri> {
        if (prefixes.trim() == "") {
            throw context.exception(XProcError.Companion.xsInvalidExcludePrefix())
        }

        val uriSet = mutableSetOf<NamespaceUri>()
        uriSet.add(NsP.namespace)
        for (token in prefixes.split("\\s+".toRegex())) {
            when (token) {
                "#all" -> {
                    for ((_, uri) in context.inscopeNamespaces) {
                        uriSet.add(uri)
                    }
                }
                "#default" -> {
                    if (context.inscopeNamespaces[""] == null) {
                        throw context.exception(XProcError.Companion.xsNoDefaultNamespace())
                    } else {
                        uriSet.add(context.inscopeNamespaces[""]!!)
                    }
                }
                else -> {
                    val uri = context.inscopeNamespaces[token]
                    if (uri == null) {
                        throw context.exception(XProcError.Companion.xsInvalidExcludePrefix(token))
                    } else {
                        uriSet.add(uri)
                    }
                }
            }
        }

        return uriSet
    }

    fun parseValues(text: String): List<XdmAtomicValue> {
        val values = mutableListOf<XdmAtomicValue>()
        val compiler = context.newXPathCompiler()
        val selector = compiler.compile(text).load()
        //selector.resourceResolver = context.pipelineConfig.documentManager
        for (value in selector.evaluate().iterator()) {
            when (value) {
                is XdmAtomicValue -> values.add(value)
                else -> throw context.exception(XProcError.Companion.xsInvalidValues(value.toString()))
            }
        }

        return values
    }

    fun parseVisibility(visible: String): Visibility {
        when (visible) {
            "private" -> return Visibility.PRIVATE
            "public" -> return Visibility.PUBLIC
            else -> throw context.exception(XProcError.Companion.xsValueDoesNotSatisfyType(visible, "Visibility"))
        }
    }

    fun parseContentTypes(text: String): List<MediaType> {
        try {
            val alist = ArrayList<MediaType>()
            for (mt in MediaType.Companion.parseList(text)) {
                alist.add(mt)
            }
            return alist
        } catch (ex: XProcException) {
            throw ex.error.asStatic().exception()
        }
    }

    fun parseSequenceType(asExpr: String): SequenceType {
        val config = context.processor.getUnderlyingConfiguration()
        val icontext = IndependentContext(config)
        icontext.clearAllNamespaces() // We get no defaults
        for ((prefix, uri) in context.inscopeNamespaces) {
            icontext.declareNamespace(prefix, uri)
        }

        try {
            icontext.setXPathLanguageLevel(31)
            val parser = XPathParser(icontext)
            val st = parser.parseSequenceType(asExpr, icontext)
            return SequenceType.fromUnderlyingSequenceType(context.processor, st)
        } catch (ex: XPathException) {
            throw context.exception(XProcError.Companion.xsInvalidSequenceType(asExpr), ex)
        }
    }

    fun xpathEq(left: XdmValue, right: XdmValue): Boolean {
        return xpathEqual(left, right, "eq")
    }

    fun xpathEqual(left: XdmValue, right: XdmValue): Boolean {
        return xpathEqual(left, right, "=")
    }

    private fun xpathEqual(left: XdmValue, right: XdmValue, op: String): Boolean {
        val compiler = context.newXPathCompiler()
        compiler.declareVariable(vara)
        compiler.declareVariable(varb)
        val exec = compiler.compile("\$a ${op} \$b")
        val selector = exec.load()
        selector.setVariable(vara, left)
        selector.setVariable(varb, right)
        return selector.effectiveBooleanValue()
    }

    fun xpathDeepEqual(left: XdmValue, right: XdmValue): Boolean {
        val compiler = context.newXPathCompiler()
        compiler.declareVariable(vara)
        compiler.declareVariable(varb)
        val exec = compiler.compile("deep-equal(\$a,\$b)")
        val selector = exec.load()
        selector.setVariable(vara, left)
        selector.setVariable(varb, right)
        return selector.effectiveBooleanValue()
    }

    fun parseAsType(value: String, type: SequenceType, inscopeNamespaces: Map<String, NamespaceUri>): XdmValue {
        val compiler = context.newXPathCompiler()
        val exec = compiler.compile(value)
        val selector = exec.load()
        val result = selector.evaluate()
        return validateAsType(result, type.underlyingSequenceType, inscopeNamespaces)
    }

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
                typeName = NsXs.integer
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

    fun xpathInstanceOf(value: XdmValue, type: QName): Boolean {
        if (type.namespaceUri != NsXs.namespace) {
            throw context.exception(XProcError.Companion.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = context.newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a instance of Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        return selector.effectiveBooleanValue()
    }

    fun xpathCastAs(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw context.exception(XProcError.Companion.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = context.newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a cast as Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        return selector.evaluate()
    }

    fun xpathTreatAs(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw context.exception(XProcError.Companion.xiImpossible("Attempt to cast to non-xs type"))
        }

        val compiler = context.newXPathCompiler()
        compiler.declareVariable(vara)
        val exec = compiler.compile("\$a treat as Q{${NsXs.namespace}}${type.localName}")
        val selector = exec.load()
        selector.setVariable(vara, value)
        try {
            return selector.evaluate()
        } catch (ex: Exception) {
            throw context.exception(XProcError.Companion.xdBadType(ex.message ?: ""), ex)
        }
    }

    fun xpathPromote(value: XdmValue, type: QName): XdmValue {
        if (type.namespaceUri != NsXs.namespace) {
            throw context.exception(XProcError.Companion.xiImpossible("Attempt to cast to non-xs type"))
        }
        if (value.underlyingValue !is AtomicValue && value != XdmEmptySequence.getInstance()) {
            throw context.exception(XProcError.Companion.xiImpossible("Attempt to promote non-atomic: ${value}"))
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
                else -> throw context.exception(XProcError.Companion.xdBadType(value.stringValue, xsdtype))
            }
            return qname
        }

        try {
            return XdmAtomicValue(value.stringValue, xsdtype)
        } catch (ex: Exception) {
            when (ex) {
                is SaxonApiException -> {
                    if (ex.message!!.contains("Invalid URI")) {
                        throw context.exception(XProcError.Companion.xdInvalidUri(value.stringValue))
                    }
                    throw context.exception(XProcError.Companion.xdBadType(value.stringValue, xsdtype))

                }
                else -> throw ex
            }
        }
    }

    fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, values: List<XdmAtomicValue>): XdmValue {
        return checkType(varName, value, sequenceType, context.inscopeNamespaces, values)
    }

    fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, inscopeNamespaces: Map<String, NamespaceUri>, values: List<XdmAtomicValue>): XdmValue {
        var newValue = value
        if (sequenceType != null) {
            val code = sequenceType.itemType.underlyingItemType.basicAlphaCode

            if (value === XdmEmptySequence.getInstance()) {
                if (sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO
                    || sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO_OR_ONE
                    || sequenceType.occurrenceIndicator == OccurrenceIndicator.ZERO_OR_MORE) {
                    return value
                }
                throw context.exception(XProcError.Companion.xdBadType("Empty sequence not allowed"))
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
        throw context.exception(XProcError.Companion.xdValueNotAllowed(value, values))
    }

    fun forceQNameKeys(inputMap: MapItem): XdmMap {
        return forceQNameKeys(inputMap, context.inscopeNamespaces)
    }

    fun forceQNameKeys(
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

    fun forceQNameKeys(inputMap: XdmMap): XdmMap {
        return forceQNameKeys(inputMap, context.inscopeNamespaces)
    }

    fun forceQNameKeys(
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
                                throw context.exception(XProcError.Companion.xdInvalidSerialization(name))
                            }
                            throw context.exception(XProcError.Companion.xdInvalidSerialization())
                        }
                    }
                } else {
                    throw context.exception(XProcError.Companion.xdInvalidSerialization())
                }
            } else {
                map = map.put(XdmAtomicValue(mapkey), value)
            }
        }
        return map
    }

    fun QNameFromStructuredQName(qname: StructuredQName): QName {
        if (qname.prefix == "") {
            return QName(qname.namespaceUri, qname.localPart)
        }
        return QName(qname.namespaceUri, "${qname.prefix}:${qname.localPart}")
    }

    // ============================================================

    private fun attributeInfo(name: QName, value: String, location: Location? = null): AttributeInfo {
        return AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
    }

    private fun fqName(name: QName): FingerprintedQName =
        FingerprintedQName(name.prefix, name.namespaceUri, name.localName)

}