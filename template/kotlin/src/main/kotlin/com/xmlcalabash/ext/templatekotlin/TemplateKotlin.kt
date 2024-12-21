package com.xmlcalabash.ext.templatekotlin

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.documents.XProcDocument.Companion.ofXml
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.XAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import kotlin.collections.plusAssign
import kotlin.inc

class TemplateKotlin(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        var binary = 0
        var byteCount = 0
        var markup = 0
        var json = 0
        var text = 0
        var lineCount = 0
        var unknown = 0

        for (doc in queues["source"]!!) {
            if (doc is XProcBinaryDocument) {
                binary++
                byteCount += doc.binaryValue.size
            } else {
                val ct = doc.contentType
                if (ct == null) {
                    unknown++
                } else {
                    if (ct.xmlContentType() || ct.htmlContentType()) {
                        markup++
                    } else if (ct.textContentType()) {
                        text++
                        lineCount += (doc.value as XdmNode).stringValue.split("\n").size
                    } else if (ct.jsonContentType()) {
                        json++
                    } else {
                        unknown++
                    }
                }
            }
        }


        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(QName("result"))

        var amap = XAttributeMap()
        if (byteCount > 0) {
            amap[QName("bytes")] = "${byteCount}"
        }
        builder.addStartElement(QName("", "binary"), amap.attributes)
        builder.addText("" + binary)
        builder.addEndElement()

        builder.addStartElement(QName("", "markup"))
        builder.addText("" + markup)
        builder.addEndElement()

        amap = XAttributeMap()
        if (lineCount > 0) {
            amap[QName("", "lines")] = "${lineCount}"
        }
        builder.addStartElement(QName("", "text"), amap.attributes)
        builder.addText("" + text)
        builder.addEndElement()

        builder.addStartElement(QName("", "json"))
        builder.addText("" + json)
        builder.addEndElement()

        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "cx:template-kotlin"
}