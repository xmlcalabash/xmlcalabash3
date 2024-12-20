package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.S9Api
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class UnwrapStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument
    var pattern = ""
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        pattern = stringBinding(Ns.match)!!
        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, pattern)
        val result = matcher.result

        // If we unwrap text, it's text. If we unwrap empty, it's XML.
        if (S9Api.isTextDocument(result) && result.stringValue.isNotEmpty()) {
            val properties = DocumentProperties(document.properties)
            properties.remove(Ns.serialization)
            receiver.output("result", XProcDocument.ofText(result, stepConfig, MediaType.TEXT, properties))
        } else {
            receiver.output("result", XProcDocument(result, stepConfig, document.properties))
        }
    }

    override fun startDocument(node: XdmNode): Boolean {
        return true
    }

    override fun endDocument(node: XdmNode) {
        // nop
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        return true
    }

    override fun attributes(
        node: XdmNode,
        matchingAttributes: AttributeMap,
        nonMatchingAttributes: AttributeMap
    ): AttributeMap? {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "attribute"))
    }

    override fun endElement(node: XdmNode) {
        // nop; if we matched, then we removed the start, so there's nothing to end
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "text"))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "comment"))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "processing-instruction"))
    }

    override fun toString(): String = "p:unwrap"
}