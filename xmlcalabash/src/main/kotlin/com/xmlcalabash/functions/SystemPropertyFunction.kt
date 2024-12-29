package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsCx
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
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue

class SystemPropertyFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "system-property")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_STRING)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_ATOMIC
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return SystemPropertyImpl()
    }

    inner class SystemPropertyImpl: ExtensionFunctionCall() {
        private var staticContext: StaticContext? = null

        override fun supplyStaticContext(context: StaticContext?, locationId: Int, arguments: Array<out Expression>?) {
            staticContext = context
        }

        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val lexicalQName = (arguments!![0].head() as Item).stringValue
            val structuredQName = StructuredQName.fromLexicalQName(lexicalQName, false, true, staticContext?.namespaceResolver)
            val propertyName = QName(structuredQName.namespaceUri, structuredQName.localPart)
            val dynamicContext = config.environment.getExecutionContext()
            val env = dynamicContext.stepConfig.environment

            val value = when (propertyName) {
                NsP.episode -> env.episode
                NsP.locale -> env.locale
                NsP.productName -> env.productName
                NsP.productVersion -> env.productVersion
                NsP.vendor -> env.vendor
                NsP.vendorUri -> env.vendorUri
                NsP.version -> "3.0 3.1" // FIXME:
                NsP.xpathVersion -> env.xpathVersion
                NsP.psviSupported -> config.processor.isSchemaAware.toString()
                NsCx.saxonVersion -> config.processor.saxonProductVersion
                NsCx.saxonEdition -> config.processor.saxonEdition
                NsCx.productBuild -> env.buildId
                else -> ""
            }

            return StringValue(value)
        }
    }
}