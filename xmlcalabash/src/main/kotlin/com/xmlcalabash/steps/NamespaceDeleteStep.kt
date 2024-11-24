package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.*
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class NamespaceDeleteStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument

    val excludeNamespaces = mutableSetOf<NamespaceUri>()
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val prefixes = stringBinding(Ns.prefixes)!!
        val prefixesContext = options[Ns.prefixes]!!.context

        for (prefix in prefixes.split("\\s+".toRegex())) {
            val uri = prefixesContext.inscopeNamespaces[prefix]
                ?: throw XProcError.xcNoNamespaceBindingForPrefix(prefix).exception()
            excludeNamespaces.add(uri)
        }

        _matcher = ProcessMatch(stepConfig, this, stepConfig.inscopeNamespaces)
        matcher.process(document.value as XdmNode, "*")
        receiver.output("result", XProcDocument.ofXml(matcher.result, stepConfig, document.properties))
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw XProcError.xiImpossible("p:namespace-delete matched start document").exception()
    }

    override fun endDocument(node: XdmNode) {
        throw XProcError.xiImpossible("p:namespace-delete matched end document").exception()
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
                throw XProcError.xcAttributeNameCollision(attrName).exception()
            }
            newMap[attrName] = attr.value
        }

        matcher.addStartElement(nodeName, stepConfig.attributeMap(newMap))
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw XProcError.xiImpossible("p:namespace-delete matched attribute").exception()
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw XProcError.xiImpossible("p:namespace-delete matched text").exception()
    }

    override fun comment(node: XdmNode) {
        throw XProcError.xiImpossible("p:namespace-delete matched comment").exception()
    }

    override fun pi(node: XdmNode) {
        throw XProcError.xiImpossible("p:namespace-delete matched processing-instruction").exception()
    }

    override fun toString(): String = "p:namespace-delete"
}