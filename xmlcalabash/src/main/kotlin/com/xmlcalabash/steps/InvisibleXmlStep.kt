package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.MediaClassification
import org.apache.logging.log4j.kotlin.logger
import org.nineml.coffeefilter.InvisibleXml

class InvisibleXmlStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val grammar = queues["grammar"]!!

        if (stepParams.stepType == NsP.ixml) {
            stepConfig.info { "The step type p:ixml is deprecated, use p:invisible-xml instead" }
        }

        val failOnError = booleanBinding(Ns.failOnError) != false
        val parameters = qnameMapBinding(Ns.parameters)

        val invisibleXml = InvisibleXml()
        val parser = if (grammar.isNotEmpty()) {
            if (grammar.size != 1) {
                throw stepConfig.exception(XProcError.Companion.xcAtMostOneGrammar())
            }
            val theGrammar = grammar.first()
            val grammarCtc = (theGrammar.contentType ?: MediaType.TEXT).classification()
            if (grammarCtc == MediaClassification.TEXT) {
                invisibleXml.getParserFromIxml(theGrammar.value.underlyingValue.stringValue)
            } else {
                throw IllegalArgumentException("Only text .ixml documents are supported")
            }
        } else {
            invisibleXml.getParser()
        }

        val sourceCtc = (source.contentType ?: MediaType.TEXT).classification()
        if (sourceCtc == MediaClassification.TEXT) {
            if (!parser.constructed()) {
                throw stepConfig.exception(XProcError.Companion.xcInvalidIxmlGrammar())
            }

            val doc = parser.parse(source.value.underlyingValue.stringValue)
            if (!doc.succeeded() && failOnError) {
                throw stepConfig.exception(XProcError.Companion.step(205))
            }

            val builder = stepConfig.processor.newDocumentBuilder()
            val bch = builder.newBuildingContentHandler()
            doc.getTree(bch)
            val tree = bch.documentNode
            receiver.output("result", XProcDocument.Companion.ofXml(tree, stepConfig))
        } else {
            throw IllegalArgumentException("Only text source documents are supported")
        }
    }

    override fun toString(): String = "p:invisible-xml"
}