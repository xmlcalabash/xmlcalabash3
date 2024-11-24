package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.Urify
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue

class UrifyFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "urify")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_ITEM, SequenceType.OPTIONAL_ITEM)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_ITEM
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return UrifyImpl()
    }

    inner class UrifyImpl: ExtensionFunctionCall() {
        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val relativeUri = arguments!![0].head().stringValue
            if (arguments.size > 1 && arguments[1].head() != null) {
                return StringValue(Urify.urify(relativeUri, arguments[1].head().stringValue))
            }
            return StringValue(Urify.urify(relativeUri))
        }
    }
}