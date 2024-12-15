package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.StaticContext
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Item
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.SequenceType

class StepAvailableFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "step-available")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_STRING)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_BOOLEAN
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return StepAvailableImpl()
    }

    inner class StepAvailableImpl: ExtensionFunctionCall() {
        private var staticContext: StaticContext? = null

        override fun supplyStaticContext(context: StaticContext?, locationId: Int, arguments: Array<out Expression>?) {
            staticContext = context
        }

        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val lexicalQName = (arguments!![0].head() as Item).stringValue
            val structuredQName = StructuredQName.fromLexicalQName(lexicalQName, false, true, staticContext?.namespaceResolver)
            val stepType = QName(structuredQName.namespaceUri, structuredQName.localPart)
            val dynamicContext = config.environment.getExecutionContext()

            val available = dynamicContext.stepAvailable(stepType)
            //println("AVAIL ${stepType}: ${available}")
            return if (available) BooleanValue.TRUE else BooleanValue.FALSE
        }
    }
}