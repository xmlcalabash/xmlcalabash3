package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.InvisibleXmlImpl
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.s9api.QName

class InvisibleXmlStep(): AbstractAtomicStep() {
    private lateinit var extensionAttr: Map<QName, String>

    override fun extensionAttributes(attributes: Map<QName, String>) {
        extensionAttr = attributes
    }

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val grammar = queues["grammar"]!!
        val implementation = extensionAttr[NsCx.processor]
            ?: System.getProperty("com.xmlcalabash.invisible-xml")
            ?: "nineml"

        if (stepParams.stepType == NsP.ixml) {
            stepConfig.info { "The step type p:ixml is deprecated, use p:invisible-xml instead" }
        }

        val failOnError = booleanBinding(Ns.failOnError) != false
        val parameters = qnameMapBinding(Ns.parameters)

        var grammarText = if (grammar.isEmpty()) {
            null
        } else {
            if (grammar.size != 1) {
                throw stepConfig.exception(XProcError.xcAtMostOneGrammar())
            }
            val theGrammar = grammar.first()
            val grammarCtc = (theGrammar.contentType ?: MediaType.TEXT).classification()
            if (grammarCtc == MediaClassification.TEXT) {
                theGrammar.value.underlyingValue.stringValue
            } else {
                throw IllegalArgumentException("Only text .ixml documents are supported")
            }
        }

        val sourceCtc = (source.contentType ?: MediaType.TEXT).classification()
        val input = if (sourceCtc == MediaClassification.TEXT) {
            source.value.underlyingValue.stringValue
        } else {
            throw IllegalArgumentException("Only text source documents are supported")
        }

        val impl = InvisibleXmlImpl(stepConfig, implementation)
        val xml = impl.parse(grammarText, input, failOnError, parameters)
        receiver.output("result", xml)
    }

    override fun toString(): String = "p:invisible-xml"
}