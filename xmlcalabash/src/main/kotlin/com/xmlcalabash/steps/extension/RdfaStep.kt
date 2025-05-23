package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.XdmAtomicValue
import org.apache.jena.datatypes.xsd.XSDDatatype.*
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFWriter
import org.semarglproject.rdf.rdfa.RdfaParser
import org.semarglproject.sink.TripleSink
import org.semarglproject.source.StreamProcessor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

class RdfaStep(): AbstractRdfStep() {
    private lateinit var dataset: Dataset
    private lateinit var model: Model

    companion object {
        private val typeMap = mapOf(
            "anyURI" to XSDanyURI,
            "base64Binary" to XSDbase64Binary,
            "boolean" to XSDboolean,
            "byte" to XSDbyte,
            "date" to XSDdate,
            "dateTime" to XSDdateTime,
            "dateTimeStamp" to XSDdateTimeStamp,
            "dayTimeDuration" to XSDdayTimeDuration,
            "decimal" to XSDdecimal,
            "double" to XSDdouble,
            "duration" to XSDduration,
            //"ENTITIES" to XSDstring, // Not XSDENTITIES, doesn't exist
            //"ENTITY" to XSDstring, // Not XSDENTITY, deprecated
            "float" to XSDfloat,
            "gDay" to XSDgDay,
            "gMonth" to XSDgMonth,
            "gMonthDay" to XSDgMonthDay,
            "gYear" to XSDgYear,
            "gYearMonth" to XSDgYearMonth,
            "hexBinary" to XSDhexBinary,
            "ID" to XSDstring, // Not XSDID, deprecated
            "IDREF" to XSDstring, // Not XSDIDREF, deprecated
            "IDREFS" to XSDstring, // Not XSDIDREFS, doesn't exist
            "int" to XSDint,
            "integer" to XSDinteger,
            "language" to XSDlanguage,
            "long" to XSDlong,
            "Name" to XSDName,
            "NCName" to XSDNCName,
            "negativeInteger" to XSDnegativeInteger,
            "NMTOKEN" to XSDNMTOKEN,
            //"NMTOKENS" to XSDstring, // Not XSDNMTOKENS, doesn't exist
            "nonNegativeInteger" to XSDnonNegativeInteger,
            "nonPositiveInteger" to XSDnonPositiveInteger,
            "normalizedString" to XSDnormalizedString,
            "NOTATION" to XSDstring, // Not XSDNOTATION, deprecated
            "positiveInteger" to XSDpositiveInteger,
            "QName" to XSDstring, // Not XSDQName, deprecated
            "short" to XSDshort,
            "string" to XSDstring,
            "time" to XSDtime,
            "token" to XSDtoken,
            "unsignedByte" to XSDunsignedByte,
            "unsignedInt" to XSDunsignedInt,
            "unsignedLong" to XSDunsignedLong,
            "unsignedShort" to XSDunsignedShort,
            "yearMonthDuration" to XSDyearMonthDuration,
        )
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()

        // Need the serialization
        val baos = ByteArrayOutputStream()
        val writer = DocumentWriter(document, baos)
        writer.set(Ns.method, "xml")
        writer.set(Ns.omitXmlDeclaration, "true")
        writer.write()

        dataset = DatasetFactory.create()
        model = dataset.defaultModel

        val bais = ByteArrayInputStream(baos.toString().toByteArray(StandardCharsets.UTF_8))

        val sink = Sink(model)
        val sp = StreamProcessor(RdfaParser.connect(sink))
        sp.process(bais, document.baseURI?.toString() ?: UriUtils.cwdAsUri().toString())

        val rdfWriter = RDFWriter.create().source(model).lang(Lang.RDFTHRIFT).build()
        val rdfBaos = ByteArrayOutputStream()
        rdfWriter.output(rdfBaos)

        receiver.output("result", XProcBinaryDocument(rdfBaos.toByteArray(), stepConfig,
            DocumentProperties(mapOf(Ns.contentType to XdmAtomicValue(MediaType.RDFTHRIFT.toString())))))
    }

    override fun toString(): String = "cx:rdfa"

    inner class Sink(val model: Model): TripleSink {
        var baseUri: URI? = null

        override fun addNonLiteral(subj: String, pred: String, obj: String) {
            val rsrc = model.createResource(subj)
            val prop = model.createProperty(pred)
            val node = model.createResource(obj)
            model.add(model.createStatement(rsrc, prop, node))
        }

        override fun addPlainLiteral(subj: String, pred: String, obj: String, lang: String?) {
            val rsrc = model.createResource(subj)
            val prop = model.createProperty(pred)
            val lit = ResourceFactory.createPlainLiteral(obj)
            model.addLiteral(rsrc, prop, lit)
        }

        override fun addTypedLiteral(subj: String, pred: String, obj: String, type: String?) {
            val rsrc = model.createResource(subj)
            val prop = model.createProperty(pred)
            if (type == null) {
                val lit = ResourceFactory.createPlainLiteral(obj)
                model.addLiteral(rsrc, prop, lit)
                return
            }

            val xstype = if (type.contains(":")) {
                val cp = type.lastIndexOf(":")
                type.substring(cp+1)
            } else {
                type
            }

            val rdftype = typeMap[xstype]
            if (rdftype != null) {
                val lit = model.createTypedLiteral(obj, rdftype)
                model.addLiteral(rsrc, prop, lit)
            } else {
                stepConfig.warn { "Unrecognized or unsupported type ignored: ${type}" }
                val lit = ResourceFactory.createPlainLiteral(obj)
                model.addLiteral(rsrc, prop, lit)
            }
        }

        override fun setBaseUri(baseUri: String?) {
            this.baseUri = URI(baseUri!!)
        }

        override fun startStream() {
            // nop
        }

        override fun endStream() {
            // nop
        }

        override fun setProperty(key: String?, value: Any?): Boolean {
            return false
        }
    }
}