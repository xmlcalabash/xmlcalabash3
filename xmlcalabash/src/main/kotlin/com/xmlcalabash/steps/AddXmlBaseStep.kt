package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI
import java.util.*

class AddXmlBaseStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument
    var baseUriStack = Stack<URI>()
    var all = false
    var relative = true
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        all = booleanBinding(Ns.all) ?: false
        relative = booleanBinding(Ns.relative) ?: true

        if (all && relative) {
            throw XProcError.xcAllAndRelative().exception()
        }

        _matcher = ProcessMatch(stepConfig,this, stepConfig.inscopeNamespaces)
        matcher.process(document.value as XdmNode, "*")

        receiver.output("result", document.with(matcher.result))
    }

    override fun startDocument(node: XdmNode): Boolean {
        return true
    }

    override fun endDocument(node: XdmNode) {
        // nop
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        val attrs = mutableMapOf<QName,String>()
        var base: URI? = null
        var addBase = all

        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName == NsXml.base) {
                base = URI(attr.stringValue)
                addBase = true
            } else {
                attrs.put(attr.nodeName, attr.stringValue)
            }
        }

        val nodeBase = node.baseURI
        if (base == null) {
            if (baseUriStack.isEmpty() || baseUriStack.peek() != nodeBase) {
                addBase = true
            }
            base = nodeBase
        }

        val saveBase = base

        if (addBase && relative && baseUriStack.isNotEmpty()) {
            base = UriUtils.makeRelativeTo(baseUriStack.peek(), base!!)
        }

        baseUriStack.push(saveBase)

        if (addBase) {
            attrs[NsXml.base] = base!!.toString()
        }
        matcher.addStartElement(node, stepConfig.attributeMap(attrs))
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw XProcError.xiImpossible("p:add-xml-base matched attribute").exception()
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
        baseUriStack.pop()
    }

    override fun text(node: XdmNode) {
        throw XProcError.xiImpossible("p:add-xml-base matched text").exception()
    }

    override fun comment(node: XdmNode) {
        throw XProcError.xiImpossible("p:add-xml-base matched comment").exception()
    }

    override fun pi(node: XdmNode) {
        throw XProcError.xiImpossible("p:add-xml-base matched processing-instruction").exception()
    }

    override fun toString(): String = "p:add-xml-base"
}