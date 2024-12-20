package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class RenameStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    companion object {
        private val MATCH = QName("match")
    }

    var document: XProcDocument? = null

    var matchPattern = "/*"
    var newName = NsCx.unusedValue
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        matchPattern = stringBinding(Ns.match)!!
        newName = qnameBinding(Ns.newName)!!

        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document!!.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document!!.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addStartElement(newName, node.underlyingNode.attributes())
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        if (matchingAttributes.size() > 1) {
            throw stepConfig.exception(XProcError.xcInvalidSelection(matchingAttributes.size()))
        }

        var amap = nonMatchingAttributes
        var nsmap = node.underlyingNode.allNamespaces
        for (attr in matchingAttributes.asList()) {
            var prefix = newName.prefix
            val uri = newName.namespaceUri
            val localName = newName.localName

            if (uri == NamespaceUri.NULL) {
                val fqName = FingerprintedQName("", NamespaceUri.NULL, localName)
                amap = amap.put(AttributeInfo(fqName, attr.type, attr.value, attr.location, attr.properties))
            } else {
                if (prefix == null || prefix == "") {
                    prefix = "_"
                }

                var count = 1
                var checkPrefix = prefix
                var nsURI = nsmap.getURIForPrefix(checkPrefix, false)
                while (nsURI != null && nsURI != uri) {
                    count++
                    checkPrefix = "${prefix}${count}"
                    nsURI = nsmap.getURIForPrefix(checkPrefix, false)
                }

                prefix = checkPrefix
                val fqName = FingerprintedQName(prefix, uri, localName)
                amap = amap.put(AttributeInfo(fqName, attr.type, attr.value, attr.location, attr.properties))
            }
        }

        return amap
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
        if (newName.namespaceUri != NamespaceUri.NULL) {
            throw stepConfig.exception(XProcError.xcBadRenamePI())
        }
        matcher.addPI(newName.localName, node.stringValue)
    }

    override fun toString(): String = "p:rename"
}