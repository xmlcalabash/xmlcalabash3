package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.FileUtils
import com.xmlcalabash.util.SaxonTreeBuilder

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
        val fos = FileUtils.outputStream(href)
        DocumentWriter(document, fos, serialization).write()
        fos.close()

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