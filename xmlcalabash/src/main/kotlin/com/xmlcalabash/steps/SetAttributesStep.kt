package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXmlns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.NodeLocation
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode

class SetAttributesStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    companion object {
        private val MATCH = QName("match")
    }

    var document: XProcDocument? = null

    var matchPattern = "/*"
    val attributeSet = mutableMapOf<QName,String?>()
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val attrMap = qnameMapBinding(Ns.attributes)
        for ((key, value) in attrMap) {
            forbidNamespaceAttribute(key)
            attributeSet[key] = (value as XdmAtomicValue).underlyingValue.stringValue
        }

        matchPattern = stringBinding(Ns.match)!!
        _matcher = processMatcher(Ns.match)
        matcher.process(document!!.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document!!.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw XProcError.xcInvalidSelection(matchPattern).exception()
    }

    override fun endDocument(node: XdmNode) {
        throw XProcError.xcInvalidSelection(matchPattern).exception()
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
        throw XProcError.xcInvalidSelection(matchPattern).exception()
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw XProcError.xcInvalidSelection(matchPattern).exception()
    }

    override fun comment(node: XdmNode) {
        throw XProcError.xcInvalidSelection(matchPattern).exception()
    }

    override fun pi(node: XdmNode) {
        throw XProcError.xcInvalidSelection(matchPattern).exception()
    }

    override fun toString(): String = "p:set-attributes"
}