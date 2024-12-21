package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.*
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.type.Untyped

open class NamespaceRenameStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var fromNS = NamespaceUri.NULL
    var toNS = NamespaceUri.NULL
    var applyTo = "all"

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()
        val document = queues["source"]!!.first()

        if (uriBinding(Ns.from) != null) {
            fromNS = NamespaceUri.of(stringBinding(Ns.from).toString())
        }
        if (uriBinding(Ns.to) != null) {
            toNS = NamespaceUri.of(stringBinding(Ns.to).toString())
        }
        applyTo = stringBinding(Ns.applyTo) ?: "all"

        _matcher = ProcessMatch(stepConfig, this, stepConfig.inscopeNamespaces)
        matcher.process(document.value as XdmNode, "*")
        receiver.output("result", XProcDocument.ofXml(matcher.result, stepConfig, document.properties))
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched start document"))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched end document"))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        val nshash = mutableMapOf<String,NamespaceUri>()
        nshash[node.nodeName.prefix] = node.nodeName.namespaceUri

        val elemPrefix = node.nodeName.prefix
        val appliesToElement = node.nodeName.namespaceUri == fromNS
        if (!appliesToElement && applyTo == "elements") {
            matcher.addStartElement(node, attributes)
            return true
        }

        val inode = node.underlyingNode

        var attrPrefix = ""
        for (ns in inode.allNamespaces) {
            nshash[ns.prefix] = ns.namespaceUri
            if (ns.namespaceUri == fromNS) {
                if (attrPrefix == "" || ns.prefix == elemPrefix) {
                    attrPrefix = ns.prefix
                }
            }
        }

        var hasAttr = false
        for (ainfo in attributes.asList()) {
            hasAttr = hasAttr || (ainfo.nodeName.namespaceUri == fromNS)
        }

        var newPrefix = if (appliesToElement) {
            elemPrefix
        } else {
            attrPrefix
        }

        if (toNS == NamespaceUri.NULL) {
            newPrefix = ""
        }

        if (elemPrefix == attrPrefix && hasAttr && fromNS != NamespaceUri.NULL && applyTo != "all") {
            var count = 1
            newPrefix = "_${count}"
            while (nshash.contains(newPrefix)) {
                count += 1
                newPrefix = "_${count}"
            }
        }

        var newNS = NamespaceMap.emptyMap()
        newNS = newNS.put(newPrefix, toNS)

        var startName = NameOfNode.makeName(inode)
        var startType = inode.schemaType
        var startAttr = inode.attributes()

        if (applyTo != "attributes" && appliesToElement) {
            startName = FingerprintedQName(newPrefix, toNS, node.nodeName.localName)
            startType = Untyped.INSTANCE
        }

        var forceAttrPrefix: String? = null
        if (applyTo == "attributes" && appliesToElement) {
            forceAttrPrefix = newPrefix
            newNS = newNS.put(elemPrefix, fromNS)
        }

        if (applyTo != "elements") {
            startAttr = patchAttributes(inode, forceAttrPrefix)
        }

        newNS = attributeNamespaceMap(newNS, startAttr)
        matcher.addStartElement(startName, startAttr, startType, newNS)

        return true
    }

    private fun patchAttributes(inode: NodeInfo, forceAttrPrefix: String?): AttributeMap {
        var startAttr = inode.attributes()

        for (attr in inode.attributes()) {
            var nameCode = attr.nodeName
            var atype = attr.type

            if (fromNS == nameCode.namespaceUri) {
                startAttr = startAttr.remove(nameCode)
                val pfx = forceAttrPrefix ?: nameCode.prefix
                nameCode = FingerprintedQName(pfx, toNS, nameCode.localPart)
                if (startAttr.get(nameCode) != null) {
                    throw stepConfig.exception(XProcError.xcAttributeNameCollision(nameCode.localPart))
                }
                atype = BuiltInAtomicType.UNTYPED_ATOMIC
            }

            startAttr = startAttr.put(AttributeInfo(nameCode, atype, attr.value, attr.location, ReceiverOption.NONE))
        }

        return startAttr
    }

    private fun attributeNamespaceMap(initialNSMap: NamespaceMap, attrmap: AttributeMap): NamespaceMap {
        var nsMap = initialNSMap
        for (attr in attrmap.asList()) {
            val pfx = attr.nodeName.prefix
            val ns = attr.nodeName.namespaceUri
            if (pfx != "") {
                nsMap = nsMap.put(pfx, ns)
            }
        }
        return nsMap
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched attribute"))
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched text"))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched comment"))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xiImpossible("p:namespace-rename matched processing-instruction"))
    }

    override fun toString(): String = "p:namespace-rename"
}