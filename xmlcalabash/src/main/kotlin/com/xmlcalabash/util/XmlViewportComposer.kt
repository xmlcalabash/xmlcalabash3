package com.xmlcalabash.util

import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class XmlViewportComposer(val stepConfig: XProcStepConfiguration, val match: String, val bindings: Map<QName,XdmValue>) {
    var itemIndex = 0

    private var _matcher: ProcessMatch? = null
    private var matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error: no matcher defined")
        set(value) {
            _matcher = value
        }

    var decomposed: XdmNode? = null
    lateinit var properties: DocumentProperties
    lateinit var contentType: MediaType
    val viewportItems = mutableListOf<XmlViewportItem>()

    fun decompose(document: XProcDocument): List<XmlViewportItem> {
        if (document.value !is XdmNode) {
            throw XProcError.xdViewportNotXml().exception()
        }

        contentType = document.contentType ?: MediaType.XML
        if (!contentType.htmlContentType() && !contentType.xmlContentType()) {
            throw XProcError.xdViewportNotXml().exception()
        }

        properties = document.properties

        val node = document.value as XdmNode
        matcher = ProcessMatch(stepConfig, Decomposer(), stepConfig.inscopeNamespaces, bindings)
        matcher.process(node, match)
        decomposed = matcher.result
        return viewportItems
    }

    fun recompose(): XProcDocument {
        matcher = ProcessMatch(stepConfig, Recomposer(), mapOf("cx" to NsCx.namespace), bindings)
        matcher.process(decomposed!!, "cx:viewport")

        // The result is text iff the match was the document node and the result is text
        if (match == "/") {
            if (S9Api.isTextDocument(matcher.result)) {
                if (properties.has(Ns.serialization)) {
                    properties.remove(Ns.serialization)
                }
                return XProcDocument.ofText(matcher.result, stepConfig, MediaType.TEXT, properties)
            } else {
                return XProcDocument.ofXml(matcher.result, stepConfig, contentType, properties)
            }
        }
        return XProcDocument.ofXml(matcher.result, stepConfig, contentType, properties)
    }

    inner class Decomposer(): ProcessMatchingNodes {
        private fun insertMarker() {
            matcher.addStartElement(NsCx.viewport, stepConfig.attributeMap(mapOf(Ns.index to itemIndex.toString())))
            itemIndex++
        }

        override fun startDocument(node: XdmNode): Boolean {
            viewportItems.add(XmlViewportItem(stepConfig, node))
            insertMarker()
            matcher.addEndElement()
            return false
        }

        override fun endDocument(node: XdmNode) {
            matcher.endDocument()
        }

        override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(node.baseURI)
            builder.addSubtree(node)
            builder.endDocument()
            viewportItems.add(XmlViewportItem(stepConfig, builder.result))
            insertMarker()
            return false
        }

        override fun attributes(
            node: XdmNode,
            matchingAttributes: AttributeMap,
            nonMatchingAttributes: AttributeMap
        ): AttributeMap? {
            throw XProcError.xdViewportOnAttribute(match).exception()
        }

        override fun endElement(node: XdmNode) {
            matcher.addEndElement()
        }

        override fun text(node: XdmNode) {
            viewportItems.add(XmlViewportItem(stepConfig, node))
            insertMarker()
            matcher.addEndElement()
        }

        override fun comment(node: XdmNode) {
            viewportItems.add(XmlViewportItem(stepConfig, node))
            insertMarker()
            matcher.addEndElement()
        }

        override fun pi(node: XdmNode) {
            viewportItems.add(XmlViewportItem(stepConfig, node))
            insertMarker()
            matcher.addEndElement()
        }
    }

    inner class Recomposer: ProcessMatchingNodes {
        private fun processMarker(marker: XdmNode) {
            val index = marker.getAttributeValue(Ns.index)!!.toInt()
            matcher.addSubtree(viewportItems[index].replacement)
        }

        override fun startDocument(node: XdmNode): Boolean {
            matcher.startDocument(node.baseURI)
            return true
        }

        override fun endDocument(node: XdmNode) {
            matcher.endDocument()
        }

        override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
            processMarker(node)
            return false
        }

        override fun attributes(
            node: XdmNode,
            matchingAttributes: AttributeMap,
            nonMatchingAttributes: AttributeMap
        ): AttributeMap? {
            throw RuntimeException("Configuration error: recomposer matched attribute")
        }

        override fun endElement(node: XdmNode) {
            // nop
        }

        override fun text(node: XdmNode) {
            throw RuntimeException("Configuration error: recomposer matched text")
        }

        override fun comment(node: XdmNode) {
            throw RuntimeException("Configuration error: recomposer matched comment")
        }

        override fun pi(node: XdmNode) {
            throw RuntimeException("Configuration error: recomposer matched pi")
        }
    }
}