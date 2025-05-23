package com.xmlcalabash.io

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsSparqlResults
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.spi.ContentTypeConverter
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.*
import org.apache.jena.atlas.web.ContentType
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.transform.stream.StreamSource

class RdfConverter(): ContentTypeConverter {
    companion object {
        val datasetTypes = listOf(MediaType.RDFTHRIFT, MediaType.NQUADS, MediaType.TRIG)

        fun rdfLang(contentType: MediaType): Lang {
            val ctype = ContentType.create(contentType.toString())
            val lang = RDFLanguages.contentTypeToLang(ctype)
            if (lang == null) {
                throw XProcError.xcxUnrecognizedRdfContentType(contentType).exception()
            }
            return lang
        }

        private val textFormatterStylesheet = "/com/xmlcalabash/sparql2text.xsl"
        private var textFormatter: XsltExecutable? = null

        private val rdfContentTypes = listOf(
            MediaType.RDFTHRIFT,
            MediaType.JSONLD,
            MediaType.N3,
            MediaType.NQUADS,
            MediaType.NTRIPLES,
            MediaType.RDFJSON,
            MediaType.RDFXML,
            MediaType.TRIG,
            MediaType.TRIX,
            MediaType.TURTLE
            )

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

    override fun canConvert(from: MediaType, to: MediaType): Boolean {
        if (from in listOf(MediaType.SPARQL_RESULTS_XML, MediaType.SPARQL_RESULTS_JSON)
            && to == MediaType.TEXT) {
            return true
        }

        return from in rdfContentTypes && to in rdfContentTypes
    }

    override fun convert(stepConfig: StepConfiguration, doc: XProcDocument, convertTo: MediaType, serialization: Map<QName, XdmValue>): XProcDocument {
        return when (doc.contentType) {
            MediaType.SPARQL_RESULTS_XML -> convertXmlSparqlToText(stepConfig, doc.value as XdmNode, serialization)
            MediaType.SPARQL_RESULTS_JSON -> convertJsonSparqlToText(stepConfig, doc, serialization)
            else -> convertToRdf(stepConfig, doc, convertTo)
        }
    }

    private fun convertToRdf(stepConfig: StepConfiguration, doc: XProcDocument, convertTo: MediaType): XProcDocument {
        val dataset = parseDocument(doc, rdfLang(doc.contentType ?: MediaType.OCTET_STREAM), null)
        val lang = rdfLang(convertTo)

        var wroteModel = false
        val baos = ByteArrayOutputStream()
        if (convertTo !in datasetTypes) {
            // Try to output only a single model, if there's only one...
            val namedModels  = mutableListOf<Resource>()
            for (model in dataset.listModelNames()) {
                namedModels.add(model)
            }
            val defModel = dataset.defaultModel

            if (namedModels.isEmpty()) {
                val writer = RDFWriter.create().source(defModel).lang(lang).build()
                writer.output(baos)
                wroteModel = true
            } else if (namedModels.size == 1) {
                val model = dataset.getNamedModel(namedModels.first())
                if (model == defModel) {
                    val writer = RDFWriter.create().source(defModel).lang(lang).build()
                    writer.output(baos)
                    wroteModel = true
                }
            }
        }

        if (!wroteModel) {
            val writer = RDFWriter.create().source(dataset).lang(lang).build()
            writer.output(baos)
        }

        if (convertTo == MediaType.RDFTHRIFT) {
            return XProcBinaryDocument(baos.toByteArray(), stepConfig)
        }

        if (convertTo.classification() in listOf(MediaClassification.XML, MediaClassification.JSON)) {
            val bais = ByteArrayInputStream(baos.toByteArray())
            val loader = DocumentLoader(stepConfig, doc.baseURI)
            val baseContentType = if (convertTo.classification() == MediaClassification.XML) {
                MediaType.XML
            } else {
                MediaType.JSON
            }
            val xmldoc = loader.load(bais, baseContentType, StandardCharsets.UTF_8)
            return xmldoc.with(convertTo)
        }

        return XProcDocument.ofText(baos.toString(StandardCharsets.UTF_8), stepConfig).with(convertTo)
    }

    private fun convertXmlSparqlToText(stepConfig: StepConfiguration, xml: XdmNode, serialization: Map<QName, XdmValue>): XProcDocument {
        val cxNumberRows = QName(NsCx.namespace, "cx:number-rows")
        val cxPageLength = QName(NsCx.namespace, "cx:page-length")
        val cxFormfeed = QName(NsCx.namespace, "cx:formfeed")
        val cxNewline = QName(NsCx.namespace, "cx:newline")

        val numberRows = QName("number-rows")
        val pageLength = QName("page-length")
        val formfeed = QName("formfeed")
        val newline = QName("nl")

        lateinit var formatter: Xslt30Transformer
        synchronized(Companion) {
            if (textFormatter == null) {
                val stream = RdfConverter::class.java.getResourceAsStream(textFormatterStylesheet)
                    ?: throw XProcError.xiCannotLoadResource(textFormatterStylesheet).exception()
                val source = StreamSource(stream)
                val compiler = stepConfig.processor.newXsltCompiler()
                compiler.isSchemaAware = stepConfig.processor.isSchemaAware
                textFormatter = compiler.compile(source)
            }
            formatter = textFormatter!!.load30()
        }

        val params = mutableMapOf<QName, XdmValue>()
        serialization[cxNumberRows]?.let { params[numberRows] = it }
        serialization[cxPageLength]?.let { params[pageLength] = it }
        serialization[cxFormfeed]?.let { params[formfeed] = it }
        serialization[cxNewline]?.let { params[newline] = it }

        formatter.setStylesheetParameters(params)
        val destination = XdmDestination()
        formatter.transform(xml.asSource(), destination)
        val text = destination.xdmNode

        return XProcDocument.ofText(text, stepConfig)
    }

    private fun convertJsonSparqlToText(stepConfig: StepConfiguration, doc: XProcDocument, serialization: Map<QName, XdmValue>): XProcDocument {
        // I have a stylesheet that does application/sparql-results+xml to text.
        // Rather than repeat all that logic here, just convert this map into the
        // appropriate XML and call that. Efficient? Maybe not. Easy? Yes.
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        val map = doc.value as XdmMap
        val head = map.get(XdmAtomicValue("head")) as XdmMap
        val results = map.get(XdmAtomicValue("results")) as XdmMap

        builder.addStartElement(NsSparqlResults.sparql)

        val varnames = mutableListOf<XdmAtomicValue>()
        builder.addStartElement(NsSparqlResults.head)
        val variables = head.get(XdmAtomicValue("vars")) as XdmArray
        for (name in variables.asList()) {
            builder.addStartElement(NsSparqlResults.variable,
                stepConfig.typeUtils.attributeMap(mapOf(Ns.name to name.underlyingValue.stringValue)))
            builder.addEndElement()
            varnames.add(name as XdmAtomicValue)
        }
        builder.addEndElement()

        builder.addStartElement(NsSparqlResults.results)
        val bindings = results.get(XdmAtomicValue("bindings")) as XdmArray
        for (binding in bindings.asList()) {
            val resultBinding = binding as XdmMap
            builder.addStartElement(NsSparqlResults.result)
            for (name in varnames) {
                val result = resultBinding.get(name) as XdmMap
                val type = result.get(XdmAtomicValue("type")) as XdmAtomicValue
                val value = result.get(XdmAtomicValue("value")) as XdmAtomicValue
                val lang = result.get(XdmAtomicValue("xml:lang")) as XdmAtomicValue?
                val datatype = result.get(XdmAtomicValue("datatype")) as XdmAtomicValue?

                val attr = mutableMapOf<QName, String?>()
                if (lang != null) {
                    attr[NsXml.lang] = lang.stringValue
                }
                if (datatype != null) {
                    attr[Ns.datatype] = datatype.stringValue
                }

                builder.addStartElement(NsSparqlResults.binding,
                    stepConfig.typeUtils.attributeMap(mapOf(Ns.name to name.stringValue)))
                builder.addStartElement(QName(NsSparqlResults.namespace, type.stringValue),
                    stepConfig.typeUtils.attributeMap(attr))
                builder.addText(value.stringValue)
                builder.addEndElement()
                builder.addEndElement()
            }
            builder.addEndElement()
        }
        builder.addEndElement()
        builder.addEndElement()
        builder.endDocument()
        val xml = builder.result

        return convertXmlSparqlToText(stepConfig, xml, serialization)
    }
}