package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.XdmNode

class DeleteStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var pattern = ""
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        pattern = stringBinding(Ns.match)!!
        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, pattern)
        val result = matcher.result

        if (S9Api.isTextDocument(result) && result.stringValue.isNotEmpty()) {
            val properties = DocumentProperties(document.properties)
            properties.remove(Ns.serialization)
            receiver.output("result", XProcDocument.ofText(result, stepConfig, MediaType.TEXT, properties))
        } else {
            receiver.output("result", XProcDocument(result, stepConfig, document.properties))
        }

    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "document"))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "document"))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        return false
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        return nonMatchingAttributes
    }

    override fun endElement(node: XdmNode) {
        // nop?
    }

    override fun text(node: XdmNode) {
        // nop
    }

    override fun comment(node: XdmNode) {
        // nop
    }

    override fun pi(node: XdmNode) {
        // nop
    }

    override fun toString(): String = "p:delete"
}