package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.StaticContext
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.value.Int64Value
import net.sf.saxon.value.SequenceType

class IterationSizeFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "iteration-size")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf()
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_ATOMIC
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return IterationPositionImpl()
    }

    inner class IterationPositionImpl: ExtensionFunctionCall() {
        private var staticContext: StaticContext? = null

        override fun supplyStaticContext(context: StaticContext?, locationId: Int, arguments: Array<out Expression>?) {
            staticContext = context
        }

        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val dynamicContext = config.getExecutionContext()
            return Int64Value(dynamicContext.iterationSize)
        }
    }
}