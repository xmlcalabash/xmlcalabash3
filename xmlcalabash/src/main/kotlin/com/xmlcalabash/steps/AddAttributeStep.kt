package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class AddAttributeStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument
    var matchPattern = "/*"
    var attName = Ns.attributeName // anything not-null, will be replaced in run()
    var attValue = ""
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()
        document = queues["source"]!!.first()

        attName = qnameBinding(Ns.attributeName)!!
        forbidNamespaceAttribute(attName)

        attValue = stringBinding(Ns.attributeValue)!!

        matchPattern = stringBinding(Ns.match)!!
        _matcher = processMatcher(Ns.match)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "document node").at(node))
    }

    override fun endDocument(node: XdmNode) {
        // I don't think this is technically possible...
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "document node"))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        val nsBindings = mutableMapOf<String,NamespaceUri>()
        if (node.nodeName.prefix != "") {
            nsBindings.put(node.nodeName.prefix, node.nodeName.namespaceUri)
        }

        val attrs = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName != attName) {
                attrs[attr.nodeName] = attr.stringValue
                if (attr.nodeName.prefix != "") {
                    nsBindings[attr.nodeName.prefix] = attr.nodeName.namespaceUri
                }
            }
        }

        // Figure out the name of the new attribute...
        val instanceAttName = if (attName.namespaceUri == NamespaceUri.NULL) {
            attName
        } else {
            var prefix = attName.prefix
            val ns = attName.namespaceUri
            if (nsBindings.containsKey(prefix) && nsBindings[prefix] != ns) {
                prefix = ""
            }
            // If there isn't a prefix, invent one
            if (prefix == "") {
                prefix = S9Api.uniquePrefix(nsBindings.keys)
            }

            QName(prefix, ns.toString(), attName.localName)
        }

        attrs[instanceAttName] = attValue
        matcher.addStartElement(node, stepConfig.attributeMap(attrs))
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "attribute"))
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "text"))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "comment"))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern, "processing-instruction"))
    }

    override fun toString(): String = "p:add-attribute"
}