package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class NamespaceDeleteStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    val excludeNamespaces = mutableSetOf<NamespaceUri>()
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val prefixes = stringBinding(Ns.prefixes)!!
        val prefixesContext = options[Ns.prefixes]!!.context

        for (prefix in prefixes.split("\\s+".toRegex())) {
            val uri = prefixesContext.inscopeNamespaces[prefix]
                ?: throw stepConfig.exception(XProcError.xcNoNamespaceBindingForPrefix(prefix))
            excludeNamespaces.add(uri)
        }

        _matcher = ProcessMatch(stepConfig, this, stepConfig.inscopeNamespaces)
        matcher.process(document.value as XdmNode, "*")
        receiver.output("result", XProcDocument.ofXml(matcher.result, stepConfig, document.properties))
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched start document"))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched end document"))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        val nodeName = if (excludeNamespaces.contains(node.nodeName.namespaceUri)) {
            QName(node.nodeName.localName)
        } else {
            node.nodeName
        }

        val newMap = mutableMapOf<QName, String?>()
        for (attr in attributes) {
            val attrName = if (excludeNamespaces.contains(attr.nodeName.namespaceUri)) {
                QName(attr.nodeName.localPart)
            } else {
                QName(attr.nodeName.prefix, attr.nodeName.namespaceUri.toString(), attr.nodeName.localPart)
            }
            if (newMap.contains(attrName)) {
                throw stepConfig.exception(XProcError.xcAttributeNameCollision(attrName))
            }
            newMap[attrName] = attr.value
        }

        matcher.addStartElement(nodeName, stepConfig.typeUtils.attributeMap(newMap))
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched attribute"))
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched text"))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched comment"))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-delete matched processing-instruction"))
    }

    override fun toString(): String = "p:namespace-delete"
}