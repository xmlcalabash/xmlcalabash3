package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.nio.file.Files
import kotlin.io.path.Path

class DetailTraceListener: StandardTraceListener() {
    val savedDocuments = mutableMapOf<Long, String>()

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        super.sendDocument(from, to, document)

        if (document.id !in savedDocuments) {
            val path = Path("/tmp/x/trace")
            val prefix = "${from.first}."
            val suffix = document.contentType?.extension() ?: ".bin"
            val tempFile = Files.createTempFile(path, prefix, suffix).toFile()
            savedDocuments[document.id] = tempFile.absolutePath

            val serializer = XProcSerializer(document.context)
            serializer.write(document, tempFile)
        }

        return document
    }

    override fun documentSummary(config: XProcStepConfiguration, builder: SaxonTreeBuilder, detail: DocumentDetail) {
        val _id = QName("id")
        val _port = QName("port")
        val _filename = QName("filename")

        val atts = mutableMapOf<QName, String>()
        atts[_id] = "${detail.id}"
        if (detail.contentType != null) {
            atts[Ns.contentType] = detail.contentType.toString()
        }
        builder.addStartElement(NsTrace.document, config.attributeMap(atts))

        builder.addStartElement(NsTrace.from, config.attributeMap(mapOf(
            _id to detail.from.first,
            _port to detail.from.second)))
        builder.addEndElement()
        builder.addStartElement(NsTrace.to, config.attributeMap(mapOf(
            _id to detail.to.first,
            _port to detail.to.second)))
        builder.addEndElement()
        builder.addStartElement(NsTrace.location, config.attributeMap(mapOf(
            _filename to savedDocuments[detail.id]!!,
        )))
        builder.addEndElement()
        builder.addEndElement()
    }

}