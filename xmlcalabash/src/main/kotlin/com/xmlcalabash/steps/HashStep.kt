package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.HashUtils
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

open class HashStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    companion object {
        private val _key = QName("key")
        private val _context = QName("context")
        private val _sharedSecret = QName("shared-secret")
        private val _senderId = QName("sender-id")
        private val _recipientId = QName("recipient-id")
    }

    lateinit var document: XProcDocument

    var value: ByteArray = byteArrayOf()
    var algorithm = NsCx.unusedValue
    var parameters = mapOf<QName, XdmValue>()
    var matchPattern = "/*"
    var version: String = ""
    var hash: String = ""

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        matchPattern = stringBinding(Ns.match)!!
        value = stringBinding(Ns.value)!!.toByteArray()
        algorithm = qnameBinding(Ns.algorithm)!!
        parameters = qnameMapBinding(Ns.parameters)
        version = if (stringBinding(Ns.version) == null) {
            when (algorithm) {
                Ns.crc -> "32"
                Ns.sha -> "1"
                Ns.md -> "5"
                else -> ""
            }
        } else {
            stringBinding(Ns.version)!!
        }

        hash = try {
            when (algorithm) {
                Ns.crc -> {
                    HashUtils.crc(value, version)
                }
                Ns.md -> {
                    HashUtils.md(value, version)
                }
                Ns.sha -> {
                    HashUtils.sha(value, version)
                }
                NsCx.hmac -> {
                    if (parameters.containsKey(Ns.accessKey)) {
                        val accessKey = parameters[Ns.accessKey].toString()
                        HashUtils.hmac(value, accessKey)
                    } else {
                        throw stepConfig.exception(XProcError.xcMissingHmacKey())
                    }
                }
                NsCx.blake3 -> {
                    // Possible parameters
                    val key = parameters[_key]
                    val sharedSecret = parameters[_sharedSecret]
                    val senderId =  parameters[_senderId]
                    val recipientId = parameters[_recipientId]

                    if (key != null && (sharedSecret != null || senderId != null || recipientId != null)) {
                        throw XProcError.xcHashBlake3ConflictingParameters().exception()
                    }

                    if (key != null) {
                        HashUtils.blake3(value, key)
                    } else {
                        if (sharedSecret != null && senderId != null && recipientId != null) {
                            HashUtils.blake3(value, sharedSecret, senderId, recipientId)
                        } else {
                            if (sharedSecret != null || senderId != null || recipientId != null) {
                                throw XProcError.xcHashBlake3IncompleteParameters().exception()
                            }
                            HashUtils.blake3(value)
                        }
                    }
                }
                else -> throw stepConfig.exception(XProcError.xcBadHashAlgorithm(algorithm.toString()))
            }
        } catch (ex: XProcException) {
            throw stepConfig.exception(ex.error)
        }

        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        matcher.addText(hash)
        return false
    }

    override fun endDocument(node: XdmNode) {
        matcher.endDocument()
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addText(hash)
        return false
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        val amap = mutableMapOf<QName, String?>()
        amap.putAll(stepConfig.attributeMap(nonMatchingAttributes))

        for (attr in matchingAttributes.asList()) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.namespaceUri.toString(), attr.nodeName.localPart)
            amap[qname] = hash
        }

        return stepConfig.attributeMap(amap)
    }

    override fun endElement(node: XdmNode) {
        // nop
    }

    override fun text(node: XdmNode) {
        matcher.addText(hash)
    }

    override fun comment(node: XdmNode) {
        matcher.addText(hash)
    }

    override fun pi(node: XdmNode) {
        matcher.addText(hash)
    }

    override fun toString(): String = "p:hash"
}