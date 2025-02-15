package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.StaticContext
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.ma.arrays.AbstractArrayItem
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.GroundedValue
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.type.Type
import net.sf.saxon.value.AtomicValue
import net.sf.saxon.value.SequenceType

class DocumentPropertiesFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "document-properties")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_ITEM)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.SINGLE_ITEM
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return DocPropImpl()
    }

    inner class DocPropImpl: ExtensionFunctionCall() {
        private var staticContext: StaticContext? = null

        override fun supplyStaticContext(context: StaticContext?, locationId: Int, arguments: Array<out Expression>?) {
            staticContext = context
        }

        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val dynamicContext = config.environment.getExecutionContext()

            val item = arguments!![0].head()
            val map = if (item is GroundedValue) {
                dynamicContext.getProperties(item)
            } else {
                null
            }

            var result = XdmMap()

            if (map == null) {
                // See if we can sort out the base URI and the content type anyway.
                when (item) {
                    is NodeInfo -> {
                        if (item.baseURI != null) {
                            result = result.put(XdmAtomicValue(Ns.baseUri), XdmAtomicValue(item.baseURI))
                        }
                        if (item.nodeKind.toShort() == Type.TEXT) {
                            result = result.put(XdmAtomicValue(Ns.contentType), XdmAtomicValue(MediaType.TEXT.toString()))
                        } else {
                            result = result.put(XdmAtomicValue(Ns.contentType), XdmAtomicValue(MediaType.XML.toString()))
                        }
                    }
                    else -> Unit
                }

                return result.underlyingValue
            }

            for (key in map.keySet()) {
                val value = map.get(key)
                result = result.put(key, value)
            }

            return result.underlyingValue
        }
    }
}