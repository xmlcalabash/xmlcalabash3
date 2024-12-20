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
import net.sf.saxon.s9api.*

class ReplaceStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument
    lateinit var replacement: XProcDocument

    var pattern = ""
    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            document = doc
        } else {
            replacement = doc
        }
    }

    override fun run() {
        super.run()

        pattern = stringBinding(Ns.match)!!
        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, pattern)

        val properties = DocumentProperties(document.properties)

        val result = matcher.result
        if (document.value is XdmNode) {
            if (S9Api.isTextDocument(result) && !S9Api.isTextDocument(document.value as XdmNode)) {
                properties[Ns.contentType] = MediaType.TEXT
                properties.remove(Ns.serialization)
            }
        }

        receiver.output("result", XProcDocument.ofXml(matcher.result, stepConfig, properties))
    }

    override fun reset() {
        super.reset()
        document = XProcDocument.ofEmpty(stepConfig)
        replacement = XProcDocument.ofEmpty(stepConfig)
    }

    override fun startDocument(node: XdmNode): Boolean {
        matcher.startDocument(node.baseURI)
        matcher.addSubtree(replacement.value)
        return false
    }

    override fun endDocument(node: XdmNode) {
        matcher.endDocument()
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addSubtree(replacement.value)
        return false
    }

    override fun attributes(node: XdmNode,
                            matchingAttributes: AttributeMap,
                            nonMatchingAttributes: AttributeMap): AttributeMap? {
        throw stepConfig.exception(XProcError.xcInvalidSelection(pattern, "attribute").at(node))
    }

    override fun endElement(node: XdmNode) {
        // nop
    }

    override fun text(node: XdmNode) {
        matcher.addSubtree(replacement.value)
    }

    override fun comment(node: XdmNode) {
        matcher.addSubtree(replacement.value)
    }

    override fun pi(node: XdmNode) {
        matcher.addSubtree(replacement.value)
    }

    override fun toString(): String = "p:replace"
}