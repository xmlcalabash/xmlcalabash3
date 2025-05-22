package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.nineml.coffeefilter.InvisibleXml

class InvisibleXmlNineML(stepConfig: XProcStepConfiguration): InvisibleXmlImpl(stepConfig, "nineml") {
    override fun parse(grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val invisibleXml = InvisibleXml()
        val parser = if (grammar != null) {
            invisibleXml.getParserFromIxml(grammar)
        } else {
            invisibleXml.getParser()
        }

        val doc = parser.parse(input)
        if (!doc.succeeded() && failOnError) {
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed())
        }

        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val bch = builder.newBuildingContentHandler()
        doc.getTree(bch)
        val tree = bch.documentNode
        return XProcDocument.ofXml(tree, stepConfig)
    }
}