package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmValue
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.net.URI

open class StoreStep(): AbstractAtomicStep() {
    var document: XProcDocument? = null

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(stringBinding(Ns.href)!!), ex)
        }

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcInvalidUri(href))
        }

        val serialization = qnameMapBinding(Ns.serialization)
        val contentType = document!!.contentType

        val serializer = XProcSerializer(stepConfig)
        val outputFile = FileOutputStream(href.path)
        serializer.write(document!!, outputFile, contentType, serialization)
        outputFile.close()

        receiver.output("result", document!!)

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