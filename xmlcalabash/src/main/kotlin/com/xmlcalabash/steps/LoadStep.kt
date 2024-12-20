package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import java.net.URISyntaxException

open class LoadStep(): AbstractAtomicStep() {
    override fun input(port: String, doc: XProcDocument) {
        // there are none
    }

    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)
        } catch (ex: Exception) {
            when (ex) {
                is URISyntaxException, is IllegalArgumentException -> {
                    val badURI = options[Ns.href]!!.value.underlyingValue.stringValue
                    throw stepConfig.exception(XProcError.xdInvalidUri(badURI), ex)
                }
                else -> throw ex
            }
        }

        val contentType = if (options.containsKey(Ns.contentType)) {
            if (options[Ns.contentType]!!.value is XdmEmptySequence) {
                null
            } else {
                mediaTypeBinding(Ns.contentType)
            }
        } else {
            null
        }

        val parameters = qnameMapBinding(Ns.parameters)
        val properties = DocumentProperties(qnameMapBinding(Ns.documentProperties))

        if (contentType != null) {
            if (properties.has(Ns.contentType)) {
                if (contentType.toString() != properties[Ns.contentType].toString()) {
                    throw stepConfig.exception(XProcError.xdContentTypesDiffer(contentType.toString(), properties[Ns.contentType].toString()))
                }
            } else {
                properties[Ns.contentType] = XdmAtomicValue(contentType.toString())
            }
        }

        val manager = stepConfig.environment.documentManager
        val document = manager.load(href!!, stepConfig, properties, parameters)

        receiver.output("result", document)
    }


    override fun toString(): String = "p:load"
}