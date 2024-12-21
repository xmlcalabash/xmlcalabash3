package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.lang.IllegalArgumentException
import java.net.URI
import java.net.URISyntaxException

open class SetPropertiesStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val merge = booleanBinding(Ns.merge) ?: true
        val setProperties = mutableMapOf<QName,XdmValue>()
        setProperties.putAll(qnameMapBinding(Ns.properties))

        if (setProperties.containsKey(Ns.contentType)) {
            throw stepConfig.exception(XProcError.xcCannotSetContentType())
        }

        var origBaseUri: URI? = document.properties.baseURI
        var newBaseUri: URI? = null

        if (setProperties.containsKey(Ns.baseUri)) {
            val uriValue = setProperties[Ns.baseUri]!!.underlyingValue.stringValue
            try {
                newBaseUri = URI(uriValue)
                if (!newBaseUri.isAbsolute) {
                    throw stepConfig.exception(XProcError.xdInvalidUri(uriValue))
                }
            } catch (ex: Exception) {
                when (ex) {
                    is URISyntaxException, is IllegalArgumentException ->
                        throw stepConfig.exception(XProcError.xdInvalidUri(uriValue))
                    else -> throw ex
                }
            }
        }

        var newDocument = document
        if (newBaseUri != null && newBaseUri != origBaseUri
            && document !is XProcBinaryDocument && document.value is XdmNode) {
            newDocument = document.with(S9Api.adjustBaseUri(document.value as XdmNode, newBaseUri))
        }

        if (setProperties.containsKey(Ns.serialization)) {
            val value = setProperties[Ns.serialization]
            if (value is XdmMap) {
                try {
                    setProperties[Ns.serialization] = stepConfig.forceQNameKeys(value.underlyingValue)
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xdInvalidSerialization(value.toString()))
                }
            } else {
                throw stepConfig.exception(XProcError.xdInvalidSerialization(value.toString()))
            }
        }

        val newProperties = DocumentProperties()
        if (merge) {
            newProperties.setAll(document.properties)
        } else {
            if (document.properties.has(Ns.contentType)) {
                newProperties[Ns.contentType] = document.properties[Ns.contentType]!!
            }
        }
        for ((name, value) in setProperties) {
            newProperties[name] = value
        }

        receiver.output("result", newDocument.with(newProperties))
    }

    override fun toString(): String = "p:set-properties"
}