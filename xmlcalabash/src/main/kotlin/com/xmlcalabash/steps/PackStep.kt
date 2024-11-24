package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap

open class PackStep(): AbstractAtomicStep() {
    private val source = mutableListOf<XProcDocument>()
    private val alternate = mutableListOf<XProcDocument>()
    private var wrapperName = NsCx.unusedValue

    override fun input(port: String, doc: XProcDocument) {
        if (port == "source") {
            source.add(doc)
        } else {
            alternate.add(doc)
        }
    }

    override fun run() {
        super.run()

        wrapperName = qnameBinding(Ns.wrapper)!!
        while (source.isNotEmpty() || alternate.isNotEmpty()) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(null)
            builder.addStartElement(wrapperName, EmptyAttributeMap.getInstance())
            if (source.isNotEmpty()) {
                builder.addSubtree(source.removeFirst().value)
            }
            if (alternate.isNotEmpty()) {
                builder.addSubtree(alternate.removeFirst().value)
            }
            builder.addEndElement()
            builder.endDocument()
            val properties = DocumentProperties()
            properties[Ns.contentType] = MediaType.XML
            receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig, properties))
        }
    }

    override fun toString(): String = "p:pack"


}