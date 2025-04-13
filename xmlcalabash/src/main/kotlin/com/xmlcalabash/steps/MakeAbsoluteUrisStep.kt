package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

open class MakeAbsoluteUrisStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var matchPattern = "*"
    var baseUri: URI? = null

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        matchPattern = stringBinding(Ns.match)!!
        baseUri = uriBinding(Ns.baseUri)

        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
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
        matcher.addStartElement(node, attributes)

        if (baseUri != null) {
            matcher.addText(baseUri!!.resolve(node.stringValue).toString())
        } else {
            if (node.baseURI != null) {
                matcher.addText(node.baseURI.resolve(node.stringValue).toString())
            }
        }

        return false
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        val amap = mutableMapOf<QName, String?>()
        amap.putAll(stepConfig.typeUtils.attributeMap(nonMatchingAttributes))

        for (attr in matchingAttributes.asList()) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.namespaceUri.toString(), attr.nodeName.localPart)
            val urivalue = if (baseUri != null) {
                baseUri!!.resolve(attr.value).toString()
            } else {
                if (node.baseURI != null) {
                    node.baseURI.resolve(attr.value).toString()
                } else {
                    ""
                }
            }
            amap[qname] = urivalue
        }

        return stepConfig.typeUtils.attributeMap(amap)
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

    override fun toString(): String = "p:make-absolute-uris"
}