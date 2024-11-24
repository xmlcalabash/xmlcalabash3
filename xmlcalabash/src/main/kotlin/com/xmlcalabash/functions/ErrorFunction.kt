package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.Int64Value
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue

// This function is used internally to raise err:XS0018 for required options that don't have
// a user-supplied value at runtime. It can raise other errors, but it's not *fully* general,
// especially with respect to the varargs on an XProcError.

class ErrorFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsCx.namespace, "error")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_STRING, SequenceType.SINGLE_INTEGER,
            SequenceType.SINGLE_ITEM, SequenceType.SINGLE_ITEM, SequenceType.SINGLE_ITEM)
    }

    override fun getMinimumNumberOfArguments(): Int {
        return 2
    }

    override fun getMaximumNumberOfArguments(): Int {
        return 5
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.EMPTY_SEQUENCE
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return ErrorImpl()
    }

    inner class ErrorImpl: ExtensionFunctionCall() {
        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val errorType = (arguments!![0].head() as StringValue).stringValue
            val errorNumber = (arguments[1].head() as Int64Value).longValue().toInt()

            val params = mutableListOf<Any>()
            for (index in 2 until arguments.size) {
                val value = arguments[index].head()
                when (value) {
                    is StringValue -> params.add(value.stringValue)
                    is QNameValue -> {
                        val name = QName(value.prefix, value.namespaceURI.toString(), value.localName)
                        params.add(name)
                    }
                    else -> params.add(value.toString())
                }
            }

            when (errorType) {
                "step" -> throw XProcError.step(errorNumber, *params.toTypedArray()).exception()
                "static" -> throw XProcError.static(errorNumber, *params.toTypedArray()).exception()
                "dynamic" -> throw XProcError.dynamic(errorNumber, *params.toTypedArray()).exception()
                else -> throw XProcError.internal(errorNumber, *params.toTypedArray()).exception()
            }
        }
    }
}