package com.xmlcalabash.ext.coffeepress

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import org.nineml.coffeefilter.InvisibleXml
import java.lang.IllegalArgumentException

class CoffeePress(): AbstractAtomicStep() {
    val grammar = mutableListOf<XProcDocument>()
    lateinit var source: XProcDocument
    lateinit var parameters: Map<QName, XdmValue>
    var failOnError = true

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            source = doc
        } else {
            grammar.add(doc)
        }
    }

    override fun run() {
        super.run()

        if (stepParams.stepType == NsP.ixml) {
            logger.info { "The step type p:ixml is deprecated, use p:invisible-xml instead" }
        }

        failOnError = booleanBinding(Ns.failOnError) ?: true
        parameters = qnameMapBinding(Ns.parameters)

        val invisibleXml = InvisibleXml()
        val parser = if (grammar.isNotEmpty()) {
            if (grammar.size != 1) {
                throw stepConfig.exception(XProcError.xcAtMostOneGrammar())
            }
            val theGrammar = grammar.first()
            if (theGrammar.contentType != null && theGrammar.contentType!!.textContentType()) {
                invisibleXml.getParserFromIxml(theGrammar.value.underlyingValue.stringValue)
            } else {
                throw IllegalArgumentException("Only text .ixml documents are supported")
            }
        } else {
            invisibleXml.getParser()
        }

        if (source.contentType != null && source.contentType!!.textContentType()) {
            if (!parser.constructed()) {
                throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar())
            }

            val doc = parser.parse(source.value.underlyingValue.stringValue)
            if (!doc.succeeded() && failOnError) {
                throw stepConfig.exception(XProcError.Companion.step(205))
            }

            val builder = stepConfig.processor.newDocumentBuilder()
            val bch = builder.newBuildingContentHandler()
            doc.getTree(bch)
            val tree = bch.documentNode
            receiver.output("result", XProcDocument.ofXml(tree, stepConfig))
        } else {
            throw IllegalArgumentException("Only text source documents are supported")
        }
    }

    override fun toString(): String = "p:invisible-xml"
}