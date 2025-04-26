package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcUserError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.QNameValue

class ErrorStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val value = options[Ns.code]!!.value.underlyingValue
        val code = when (value) {
            is QName -> value
            is QNameValue -> QName(value.structuredQName)
            else -> throw RuntimeException("Unexpected type, not a qname: ${value}")
        }

        var pPrefix: String? = null
        val namespaces = mutableMapOf<String,NamespaceUri>()
        for ((prefix, ns) in stepConfig.inscopeNamespaces) {
            namespaces[prefix] = ns
            if (prefix != "" && NsP.namespace == ns) {
                pPrefix = prefix
            }
        }
        if (pPrefix == null) {
            if (stepConfig.inscopeNamespaces.containsKey("p")) {
                var count = 1
                pPrefix = "_${count++}"
                while (stepConfig.inscopeNamespaces.containsKey(pPrefix)) {
                    pPrefix = "_${count++}"
                }
            } else {
                pPrefix = "p"
            }
            namespaces[pPrefix] = NsP.namespace
        }

        var nsmap = NamespaceMap.emptyMap()
        for ((prefix, uri) in namespaces) {
            nsmap = nsmap.put(prefix, uri)
        }

        val map = mutableMapOf(
            QName("code") to code.toString(),
            QName("type") to "${pPrefix}:error"
        )

        val sources = queues["source"]
        val documents = if (sources == null || sources.isEmpty()) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(stepConfig.baseUri)
            builder.endDocument()
            listOf(XProcDocument.ofXml(builder.result, stepConfig))
        } else {
            sources
        }

        val location = if (sources == null || sources.isEmpty()) {
            Location.NULL
        } else {
            Location(sources.first())
        }

        throw XProcUserError(code, stepParams.location, location, *documents.toTypedArray()).exception()
    }

    override fun toString(): String = "p:error"
}