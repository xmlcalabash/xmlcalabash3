package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap

open class PackStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        val source = mutableListOf<XProcDocument>()
        source.addAll(queues["source"]!!)
        val alternate = mutableListOf<XProcDocument>()
        alternate.addAll(queues["alternate"]!!)
        val wrapperName = qnameBinding(Ns.wrapper)!!

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