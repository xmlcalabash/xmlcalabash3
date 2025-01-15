package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.FileUtils
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

class DetailTraceListener(val path: Path): StandardTraceListener() {
    val savedDocuments = mutableMapOf<Long, String>()

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        super.sendDocument(from, to, document)

        if (document.id !in savedDocuments) {
            val prefix = "${from.first.id}."
            val suffix = document.contentType?.extension() ?: ".bin"
            val tempFile = Files.createTempFile(path, prefix, suffix).toFile()
            savedDocuments[document.id] = tempFile.absolutePath

            val fos = FileUtils.outputStream(tempFile)
            try {
                val writer = DocumentWriter(document, fos)
                writer.set(Ns.method, "adaptive")
                writer.write()
            } catch (ex: Exception) {
                logger.warn { "Failed to write trace document: ${ex.message}"}
            }
            fos.close()
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