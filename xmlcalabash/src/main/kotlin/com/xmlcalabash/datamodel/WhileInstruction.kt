package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.*

class WhileInstruction(parent: XProcInstruction): CompoundLoopDeclaration(parent, NsCx.`while`) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '1', NsP.output to '1')
    private var _test: String? = null

    val testDefined: Boolean
        get() {
            return _test != null
        }

    var test: String
        get() {
            return _test!!
        }
        set(value) {
            checkOpen()
            _test = value
        }

    override fun elaborateInstructions() {
        if (!testDefined) {
            throw stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.test))
        }

        val withOption = WithOptionInstruction(this, Ns.test, stepConfig)
        withOption.select = XProcExpression.avt(stepConfig, test)
        _children.add(0, withOption)

        super.elaborateInstructions()

        // The output port on viewport is always "result"
        val output = children.filterIsInstance<OutputInstruction>().firstOrNull()
        if (output == null) {
            throw stepConfig.exception(XProcError.xsNoOutputConnection("result"))
        } else {
            output._port = "result"
        }
    }
}