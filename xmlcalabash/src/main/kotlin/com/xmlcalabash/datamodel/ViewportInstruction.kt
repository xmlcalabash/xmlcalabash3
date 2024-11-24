package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmNode

class ViewportInstruction(parent: XProcInstruction): CompoundLoopDeclaration(parent, NsP.viewport) {
    companion object {
        private val INVALID_MATCH = "*** no match pattern specified ***"
    }

    override val contentModel = anySteps + mapOf(NsP.withInput to '1', NsP.output to '1')

    val matchDefined: Boolean
        get() {
            return match != INVALID_MATCH
        }

    var match: String = INVALID_MATCH
        set(value) {
            checkOpen()
            field = value
        }

    override fun elaborateInstructions() {
        if (!matchDefined) {
            throw XProcError.xsMissingRequiredAttribute(Ns.match).exception()
        }

        try {
            val matcher = ProcessMatch(stepConfig, DummyProcessor(), stepConfig.inscopeNamespaces, emptyMap())
            matcher.compilePattern(match)
        } catch (ex: SaxonApiException) {
            throw XProcError.xsXPathStaticError("Invalid match pattern: ${match}").exception(ex)
        }

        super.elaborateInstructions()

        // The output port on viewport is always "result"
        val output = children.filterIsInstance<OutputInstruction>().first()
        output._port = "result"
    }

    private class DummyProcessor: ProcessMatchingNodes {
        override fun startDocument(node: XdmNode): Boolean {
            TODO("This should never be called")
        }

        override fun endDocument(node: XdmNode) {
            TODO("This should never be called")
        }

        override fun startElement(
            node: XdmNode,
            attributes: AttributeMap
        ): Boolean {
            TODO("This should never be called")
        }

        override fun attributes(
            node: XdmNode,
            matchingAttributes: AttributeMap,
            nonMatchingAttributes: AttributeMap
        ): AttributeMap? {
            TODO("This should never be called")
        }

        override fun endElement(node: XdmNode) {
            TODO("This should never be called")
        }

        override fun text(node: XdmNode) {
            TODO("This should never be called")
        }

        override fun comment(node: XdmNode) {
            TODO("This should never be called")
        }

        override fun pi(node: XdmNode) {
            TODO("This should never be called")
        }
    }
}