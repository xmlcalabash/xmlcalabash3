package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.*

class ViewportInstruction(parent: XProcInstruction): CompoundLoopDeclaration(parent, NsP.viewport) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '1', NsP.output to '1')
    private var _match: XProcMatchExpression? = null

    val matchDefined: Boolean
        get() {
            return _match != null
        }

    var match: XProcMatchExpression
        get() {
            return _match!!
        }
        set(value) {
            checkOpen()
            _match = value
        }

    override fun elaborateInstructions() {
        if (!matchDefined) {
            throw stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.match))
        }

        val withOption = WithOptionInstruction(this, Ns.match, stepConfig)
        withOption.select = match
        withOption.specialType = SpecialType.XSLT_SELECTION_PATTERN
        _children.add(withOption)

        try {
            val variables = mutableMapOf<QName, XdmValue>()
            for (name in match.details.variableRefs) {
                val binding = stepConfig.inscopeVariables[name]
                if (binding != null) {
                    variables[name] = XdmAtomicValue(0) // value is irrelevant for this compile test
                } else {
                    throw stepConfig.exception(XProcError.xsXPathStaticError(name))
                }
            }
            val matcher = ProcessMatch(stepConfig, DummyProcessor(), stepConfig.inscopeNamespaces, variables)
            matcher.compilePattern(match.match)
        } catch (ex: SaxonApiException) {
            throw stepConfig.exception(XProcError.xsXPathStaticError("Invalid match pattern: ${match}"), ex)
        }

        super.elaborateInstructions()

        // The output port on viewport is always "result"
        val output = children.filterIsInstance<OutputInstruction>().firstOrNull()
        if (output == null) {
            throw stepConfig.exception(XProcError.xsNoOutputConnection("result"))
        } else {
            output._port = "result"
        }
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