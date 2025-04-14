package com.xmlcalabash.ext.uniqueid

import com.github.f4b6a3.uuid.UuidCreator
import com.github.f4b6a3.uuid.enums.UuidLocalDomain
import com.github.f4b6a3.uuid.enums.UuidNamespace
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.util.*

class UniqueIdStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    companion object {
        private val _flavor = QName("flavor")
        private val _sequential = QName("sequential")
        private val _version = QName("version")
        private val _domain = QName("domain")
        private val _namespace = QName("namespace")
        private val _value = QName("value")
        private val _type = QName("type")
    }

    var matchPattern = "/*"
    val parameters = mutableMapOf<QName, XdmValue>()
    var flavor = "uuid"
    var sequential = false
    var identifier: String? = null
    lateinit var nextid: () -> String

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        matchPattern = stringBinding(Ns.match)!!
        parameters.putAll(qnameMapBinding(Ns.parameters))
        flavor = stringBinding(_flavor) ?: "uuid"
        sequential = booleanBinding(_sequential) ?: false

        when (flavor) {
            "uuid" -> {
                val version = (parameters[_version] ?: XdmAtomicValue(4)).underlyingValue.stringValue.toInt()
                var nextUuid: () -> UUID = { throw stepConfig.exception(XProcError.xcUnsupportedUuidVersion(version)) }
                when (version) {
                    1 -> nextUuid = { UuidCreator.getTimeBased() }
                    2 -> {
                        val domainParam = parameters[_domain]?.underlyingValue?.stringValue ?: "user"
                        val domain = when (domainParam) {
                            "user" -> UuidLocalDomain.LOCAL_DOMAIN_PERSON.value
                            "group" -> UuidLocalDomain.LOCAL_DOMAIN_GROUP.value
                            "org", "organization" -> UuidLocalDomain.LOCAL_DOMAIN_ORG.value
                            else -> {
                                domainParam.toInt().toByte()
                            }
                        }
                        val value = parameters[_value]!!.underlyingValue.stringValue.toInt()
                        nextUuid = { UuidCreator.getDceSecurity(domain, value) }
                    }
                    3, 5 -> {
                        val nsParam = parameters[_namespace]?.underlyingValue?.stringValue ?: throw RuntimeException("Namespace not found")
                        val ns = when (nsParam) {
                            "url", "uri" -> UuidNamespace.NAMESPACE_URL.value
                            "dns" -> UuidNamespace.NAMESPACE_DNS.value
                            "iso-oid", "oid" -> UuidNamespace.NAMESPACE_OID.value
                            "x500" -> UuidNamespace.NAMESPACE_X500.value
                            else -> UUID.fromString(nsParam)
                        }
                        val value = parameters[_value]?.underlyingValue?.stringValue ?: throw RuntimeException("value not found")
                        if (version == 3) {
                            nextUuid = { UuidCreator.getNameBasedMd5(ns, value) }
                        } else {
                            nextUuid = { UuidCreator.getNameBasedSha1(ns, value) }
                        }
                    }
                    4 -> nextUuid = { UuidCreator.getRandomBased() }
                    6 -> nextUuid = { UuidCreator.getTimeOrdered() }
                    7 -> nextUuid = { UuidCreator.getTimeOrderedEpoch() }
                    else -> Unit
                }

                nextid = { nextUuid().toString() }
            }
            "ulid" -> {
                nextid = { ULID.next() }
            }
            "typeid" -> {
                val prefix = parameters[_type]?.underlyingValue?.stringValue ?: ""
                if (!prefix.matches("^([a-z]([a-z_]{0,61}[a-z])?)?$".toRegex())) {
                    throw RuntimeException("Invalid type: ${prefix}")
                }
                nextid = { generateTypeId(prefix) }
            }
            else -> {
                // In theory, this can't happen because the flavor value has been checked already
                nextid = { throw RuntimeException("Unknow identifier flavor: ${flavor}")}
            }
        }

        _matcher = ProcessMatch(stepConfig,this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        matcher.addText(nextId())
        return false
    }

    override fun endDocument(node: XdmNode) {
        matcher.endDocument()
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addText(nextId())
        return false
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        val amap = mutableMapOf<QName, String?>()
        amap.putAll(stepConfig.typeUtils.attributeMap(nonMatchingAttributes))

        for (attr in matchingAttributes.asList()) {
            val lexical = attr.nodeName.displayName
            val ns = attr.nodeName.namespaceUri
            val qname = QName(ns, lexical)
            amap[qname] = nextId()
        }

        return stepConfig.typeUtils.attributeMap(amap)
    }

    override fun endElement(node: XdmNode) {
        // nop
    }

    override fun text(node: XdmNode) {
        matcher.addText(nextId())
    }

    override fun comment(node: XdmNode) {
        matcher.addText(nextId())
    }

    override fun pi(node: XdmNode) {
        matcher.addText(nextId())
    }

    private fun nextId(): String {
        if (identifier == null || sequential) {
            identifier = nextid()
        }
        return identifier!!
    }

    private fun generateTypeId(prefix: String): String {
        val mapping = arrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z')
        val digits = Array<Char>(26) { '0' }

        val uuid = UuidCreator.getTimeOrderedEpoch()
        var high = uuid.mostSignificantBits.toULong()
        var low = uuid.leastSignificantBits.toULong()

        // Now I want groups of five bits bearing in mind that the 128 isn't divisible by five.
        // Pretend we have 130 bits padded on the *left* with two zeros. That's 12 groups of
        // five, then a group of 4 plus the lsb of the high group, followed by 12 groups of five.
        var pos = 25
        for (count in 1..12) {
            val digit = low and 0x1Fu
            digits[pos] = mapping[digit.toInt()]
            //println("${pos} ${low.toString(2).padStart(64, '0')} ${digit.toString(2).padStart(5, '0')}")
            pos -= 1
            low = low shr 5
        }

        val lsb = (high and 0x1u) shl 4
        high = high shr 1

        low = low or lsb

        //println(low.toString(2).padStart(64, '0'))

        digits[pos--] = mapping[low.toInt()]

        for (count in 1..13) {
            val digit = high and 0x1Fu
            digits[pos] = mapping[digit.toInt()]
            //println("${pos} ${low.toString(2).padStart(64, '0')} ${digit.toString(2).padStart(5, '0')}")
            pos -= 1
            high = high shr 5
        }

        if (prefix == "") {
            return digits.joinToString("")
        } else {
            return "${prefix}_${digits.joinToString("")}"
        }
    }

    override fun toString(): String = "cx:unique-id"
}