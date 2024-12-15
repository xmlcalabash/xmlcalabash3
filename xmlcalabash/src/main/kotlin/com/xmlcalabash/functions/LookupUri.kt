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

class LookupUri(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "lookup-uri")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_STRING)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_STRING
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return lookupUriImpl()
    }

    inner class lookupUriImpl: ExtensionFunctionCall() {
        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val uri = arguments!![0].head().stringValue

            val dynamicContext = config.environment.getExecutionContext()
            config.environment.documentManager

            return StringValue(uri)
        }
    }
}