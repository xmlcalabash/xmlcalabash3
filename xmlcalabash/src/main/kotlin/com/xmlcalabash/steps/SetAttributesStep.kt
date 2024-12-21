package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode

class SetAttributesStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var matchPattern = "/*"
    val attributeSet = mutableMapOf<QName,String?>()
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val attrMap = qnameMapBinding(Ns.attributes)
        for ((key, value) in attrMap) {
            forbidNamespaceAttribute(key)
            attributeSet[key] = (value as XdmAtomicValue).underlyingValue.stringValue
        }

        matchPattern = stringBinding(Ns.match)!!
        _matcher = processMatcher(Ns.match)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        val newAttr = mutableMapOf<QName, String?>()
        newAttr.putAll(stepConfig.attributeMap(attributes))
        for ((name, value) in attributeSet) {
            newAttr[name] = value
        }
        matcher.addStartElement(node, stepConfig.attributeMap(newAttr))
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun toString(): String = "p:set-attributes"
}