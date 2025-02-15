package com.xmlcalabash.ext.rdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.s9api.QName
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI

abstract class AbstractRdfStep(): AbstractAtomicStep() {
    companion object {
        val _graph = QName("graph")
        val _language = QName("language")
    }

    fun parseLanguage(language: String?): Lang? {
        if (language == null) {
            return null
        }

        return when (language) {
            "turtle", "ttl", "n3" -> Lang.TURTLE
            "n-triples", "ntriples" -> Lang.NTRIPLES
            "json-ld", "jsonld" -> Lang.JSONLD
            "rdf-json", "rdfjson" -> Lang.RDFJSON
            "trig" -> Lang.TRIG
            "n-quads", "nquads" -> Lang.NQUADS
            "rdf-thrift", "rdfthrift" -> Lang.RDFTHRIFT
            "rdf-xml", "rdfxml", "rdf/xml" -> Lang.RDFXML
            else -> {
                throw IllegalArgumentException("Unknown language $language")
            }
        }
    }

    fun parseDocument(doc: XProcDocument, userLang: Lang?, graph: URI? = null, dataset: Dataset = DatasetFactory.create()): Dataset {
        val ctype = doc.contentType ?: MediaType.OCTET_STREAM
        val lang = userLang ?: RdfConverter.rdfLang(ctype)

        val stream = when (doc.contentType!!.classification()) {
            MediaClassification.BINARY -> ByteArrayInputStream((doc as XProcBinaryDocument).binaryValue)
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos)
                writer.set(Ns.method, "xml")
                writer.set(Ns.encoding, "UTF-8")
                writer.write()
                ByteArrayInputStream(baos.toByteArray())
            }
            MediaClassification.TEXT -> {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos)
                writer.set(Ns.method, "text")
                writer.set(Ns.encoding, "UTF-8")
                writer.write()
                ByteArrayInputStream(baos.toByteArray())
            }
            else -> {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos)
                writer.set(Ns.method, "json")
                writer.set(Ns.encoding, "UTF-8")
                writer.write()
                ByteArrayInputStream(baos.toByteArray())
            }
        }

        val parser = RDFParser.create().source(stream).lang(lang).build()
        parser.parse(dataset)

        if (graph != null) {
            val model = dataset.getNamedModel(graph.toString())
            return DatasetFactory.create(model)
        }
        return dataset
    }
}