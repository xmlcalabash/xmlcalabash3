package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.StaticContext
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.functions.AccessorFn
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.value.EmptySequence
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue
import java.net.URI

class DocumentPropertyFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "document-property")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.SINGLE_ITEM, SequenceType.SINGLE_ITEM)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.OPTIONAL_ITEM
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
            val dynamicContext = config.getExecutionContext()

            val item = arguments!![0].head()
            val propname = arguments[1].head()

            val prop = when (propname) {
                is QNameValue -> {
                    QName(propname.getComponent(AccessorFn.Component.NAMESPACE).stringValue,
                        propname.getComponent(AccessorFn.Component.LOCALNAME).stringValue)
                }
                is StringValue -> {
                    try {
                        val sval = propname.stringValue
                        if (sval.startsWith("Q{")) {
                            val pos = sval.indexOf("}")
                            val uri = sval.substring(2, pos)
                            val local = parseNCName(sval.substring(pos+1))
                            QName("", uri, local)
                        } else {
                            if (sval.contains(":")) {
                                val pos = sval.indexOf(":")
                                val pfx = parseNCName(sval.substring(0, pos))
                                val local = parseNCName(sval.substring(pos+1))
                                val uri = staticContext!!.namespaceResolver.getURIForPrefix(pfx, false)
                                    ?: throw XProcError.xdInvalidDocumentPropertyQName(sval).exception()
                                QName(pfx, uri.toString(), local)
                            } else {
                                QName("", sval)
                            }
                        }
                    } catch (ex: XProcException) {
                        if (ex.error.code == NsErr.xd(36)) {
                            throw XProcError.xdInvalidDocumentPropertyQName(ex.error.details[0] as String).exception()
                        }
                        throw ex
                    }
                }
                else -> throw RuntimeException("Unexpected key in document-property map: ${propname}")
            }

            val map = dynamicContext.getProperties(item)
            if (map != null) {
                val value = map.get(XdmAtomicValue(prop)) ?: return EmptySequence.getInstance()

                if (prop == Ns.baseUri) {
                    // Horrible hack that's a consequence of the business about document-uri() values
                    val pos = value.underlyingValue.stringValue.indexOf("?inline_id=")
                    if (pos >= 0) {
                        val fixedURI = value.underlyingValue.stringValue.substring(0, pos)
                        return XdmAtomicValue(URI(fixedURI)).underlyingValue
                    }
                }

                return value.underlyingValue
            }

            return EmptySequence.getInstance()
        }
    }

    private fun parseNCName(name: String): String {
        if (name.matches("[\\p{L}|_]+".toRegex())) {
            return name
        }
        throw XProcError.xdBadType(name, "NCName").exception()
    }
}