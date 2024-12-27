package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.SequenceType
import org.apache.logging.log4j.kotlin.logger

class FunctionLibraryImportableFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "function-library-importable")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_STRING)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_BOOLEAN
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return FuncLibImportableImpl()
    }

    inner class FuncLibImportableImpl: ExtensionFunctionCall() {
        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            if (config.processor.underlyingConfiguration.editionCode != "EE") {
                logger.warn { "Importing functions requires Saxon EE" }
                return BooleanValue.FALSE
            }

            val item = arguments!![0].head().stringValue
            if (listOf("application/xslt+xml", "text/xsl", "text/xslt", "application/xquery").contains(item)) {
                return BooleanValue.TRUE
            }

            return BooleanValue.FALSE
        }
    }
}