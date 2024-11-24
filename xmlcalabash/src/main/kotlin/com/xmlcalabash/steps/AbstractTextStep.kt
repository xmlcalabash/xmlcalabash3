package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

abstract class AbstractTextStep(): AbstractAtomicStep() {
    lateinit var source: XProcDocument

    override fun input(port: String, doc: XProcDocument) {
        source = doc
    }

    fun text(doc: XProcDocument = source): String {
        return S9Api.textContent(doc)
    }

    fun textLines(doc: XProcDocument = source): List<String> {
        var text = text(doc)

        text = text.replace("\r\n", "\n")
        text = text.replace("\r", "\n")
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length - 1)
        }
        if (text.isEmpty()) {
            return listOf()
        }
        return text.split("\n")
    }

    fun textNode(text: String, baseURI: URI?): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseURI)
        builder.addSubtree(XdmAtomicValue(text))
        builder.endDocument()
        return builder.result
    }

    override fun toString(): String = "p:text-count"
}