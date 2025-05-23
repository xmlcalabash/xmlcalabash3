package com.xmlcalabash.io

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.spi.ContentTypeLoader
import com.xmlcalabash.spi.ContentTypeLoaderProvider
import net.sf.saxon.s9api.XdmAtomicValue
import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class RdfLoader(): ContentTypeLoader, ContentTypeLoaderProvider {
    override fun contentTypes(): List<MediaType> {
        return listOf(
            // Do not add RDF/XML, TriX, JSON-LD, or RDF-JSON to this list;
            // those are XML or JSON formats and it's perfectly reasonable
            // to want to process them as XML or JSON in a pipeline.
            MediaType.RDFTHRIFT,
            MediaType.N3,
            MediaType.NQUADS,
            MediaType.NTRIPLES,
            MediaType.TRIG,
            MediaType.TURTLE
        )
    }

    override fun load(context: StepConfiguration, uri: URI?, inputStream: InputStream, contentType: MediaType, inputCharset: Charset?): XProcDocument {
        val lang = RdfConverter.rdfLang(contentType)
        val stream = if (lang == Lang.RDFTHRIFT) {
            inputStream
        } else {
            val input = DocumentLoader.readTextStream(inputStream, inputCharset)
            ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
        }

        val dataset = DatasetFactory.create()
        val parser = RDFParser.create().base(uri?.toString()).source(stream).lang(lang).build()
        parser.parse(dataset)

        val writer = RDFWriter.create().source(dataset).lang(Lang.RDFTHRIFT).build()
        var baos = ByteArrayOutputStream()
        writer.output(baos)

        return XProcBinaryDocument(
            baos.toByteArray(), context,
            DocumentProperties(mapOf(Ns.contentType to XdmAtomicValue(MediaType.RDFTHRIFT.toString())))
        )
    }

    override fun create(): ContentTypeLoader {
        return this
    }
}