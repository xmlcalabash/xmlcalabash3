package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.SaxonTreeBuilder
import java.io.File
import java.io.FileOutputStream

open class StoreStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(stringBinding(Ns.href)!!), ex)
        }

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcInvalidUri(href))
        }

        val serialization = qnameMapBinding(Ns.serialization)
        val contentType = document.contentType ?: MediaType.OCTET_STREAM

        val serializer = XProcSerializer(stepConfig)
        serializer.setDefaultProperties(contentType, serialization)

        val outFile = File(href.path)
        serializer.write(document, outFile, contentType)

        receiver.output("result", document)

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result)
        builder.addText(href.toString())
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result-uri", XProcDocument(builder.result, stepConfig))
    }

    override fun toString(): String = "p:store"
}