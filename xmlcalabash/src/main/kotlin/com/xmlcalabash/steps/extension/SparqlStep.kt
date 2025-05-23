package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.RdfConverter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsSparqlResults
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import org.apache.jena.query.DatasetFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFWriter
import org.apache.jena.sparql.resultset.RDFOutput
import java.io.ByteArrayOutputStream

class SparqlStep(): AbstractRdfStep() {
    override fun run() {
        super.run()

        val dataDoc = queues["source"]!!.first()
        val queryDoc = queues["query"]!!.first()
        val contentType = mediaTypeBinding(Ns.contentType, MediaType.SPARQL_RESULTS_XML)

        val dataset = RdfConverter.parseDocument(dataDoc, null, null)

        val queryString = (queryDoc.value as XdmNode).underlyingValue.stringValue

        val query = QueryFactory.create(queryString)
        val exec = QueryExecutionFactory.create(query, dataset)
        val queryResults = exec.execSelect()

        when (contentType) {
            MediaType.SPARQL_RESULTS_XML -> returnXml(queryResults)
            MediaType.SPARQL_RESULTS_JSON -> returnJson(queryResults)
            MediaType.RDFTHRIFT -> returnThrift(queryResults)
            else -> throw stepConfig.exception(XProcError.xcxUnsupportedContentType(contentType))
        }
    }

    fun returnThrift(queryResults: ResultSet) {
        val model = RDFOutput.encodeAsModel(queryResults)
        val resultset = DatasetFactory.create(model)

        val writer = RDFWriter.create().source(resultset).lang(Lang.RDFTHRIFT).build()
        var baos = ByteArrayOutputStream()
        writer.output(baos)

        val doc = XProcBinaryDocument(
            baos.toByteArray(), stepConfig,
            DocumentProperties(mapOf(Ns.contentType to XdmAtomicValue(MediaType.RDFTHRIFT.toString())))
        )
        receiver.output("result", doc)
    }

    fun returnXml(queryResults: ResultSet) {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsSparqlResults.sparql)

        builder.addStartElement(NsSparqlResults.head)
        for (variable in queryResults.resultVars) {
            builder.addStartElement(
                NsSparqlResults.variable,
                stepConfig.typeUtils.attributeMap(mapOf(Ns.name to variable)))
            builder.addEndElement()
        }
        builder.addEndElement()

        builder.addStartElement(NsSparqlResults.results)
        for (soln in queryResults) {
            builder.addStartElement(NsSparqlResults.result)

            for (variable in soln.varNames()) {
                builder.addStartElement(
                    NsSparqlResults.binding,
                    stepConfig.typeUtils.attributeMap(mapOf(Ns.name to variable)))

                val node = soln.get(variable)
                if (node.isLiteral) {
                    val lit = node.asLiteral()
                    val attr = mutableMapOf<QName, String?>()
                    if (lit.language == null || lit.language == "") {
                        val dt = lit.datatypeURI
                        if (dt != null && !"".equals(dt)) {
                            attr[Ns.datatype] = dt.toString()
                        }
                    } else {
                        attr[NsXml.lang] = node.asLiteral().language
                    }

                    builder.addStartElement(NsSparqlResults.literal, stepConfig.typeUtils.attributeMap(attr))
                    builder.addText(node.asLiteral().toString())
                    builder.addEndElement()
                } else if (node.isResource) {
                    val rsrc = node.asResource()
                    if (rsrc.isAnon) {
                        builder.addStartElement(NsSparqlResults.bnode)
                        builder.addText(rsrc.toString())
                        builder.addEndElement()
                    } else {
                        builder.addStartElement(NsSparqlResults.uri)
                        builder.addText(rsrc.toString())
                        builder.addEndElement()
                    }
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Unexpected node type in sparql results"))
                }

                builder.addEndElement()
            }

            builder.addEndElement()
        }
        builder.addEndElement()

        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig, MediaType.SPARQL_RESULTS_XML))
    }

    fun returnJson(queryResults: ResultSet) {
        var json = XdmMap()
        var head = XdmMap()
        var variables = XdmArray()
        for (variable in queryResults.resultVars) {
            variables = variables.addMember(XdmAtomicValue(variable))
        }
        head = head.put(XdmAtomicValue("vars"), variables)
        json = json.put(XdmAtomicValue("head"), head)

        var results = XdmMap()
        var bindings = XdmArray()

        for (soln in queryResults) {
            var binding = XdmMap()
            for (variable in soln.varNames()) {
                var result = XdmMap()

                val node = soln.get(variable)
                if (node.isLiteral) {
                    result = result.put(XdmAtomicValue("type"), XdmAtomicValue("literal"))

                    val lit = node.asLiteral()
                    if (lit.language == null || lit.language == "") {
                        val dt = lit.datatypeURI
                        if (dt != null && !"".equals(dt)) {
                            result = result.put(XdmAtomicValue("datatype"), XdmAtomicValue(dt.toString()))
                        }
                    } else {
                        result = result.put(XdmAtomicValue("xml:lang"), XdmAtomicValue(node.asLiteral().language))
                    }

                    result = result.put(XdmAtomicValue("value"), XdmAtomicValue(lit.toString()))
                } else if (node.isResource) {
                    val rsrc = node.asResource()
                    if (rsrc.isAnon) {
                        result = result.put(XdmAtomicValue("type"), XdmAtomicValue("bnode"))
                        result = result.put(XdmAtomicValue("value"), XdmAtomicValue(rsrc.toString()))
                    } else {
                        result = result.put(XdmAtomicValue("type"), XdmAtomicValue("uri"))
                        result = result.put(XdmAtomicValue("value"), XdmAtomicValue(rsrc.toString()))
                    }
                } else {
                    throw stepConfig.exception(XProcError.xiImpossible("Unexpected node type in sparql results"))
                }

                binding = binding.put(XdmAtomicValue(variable), result)
            }

            bindings = bindings.addMember(binding)
        }

        results = results.put(XdmAtomicValue("bindings"), bindings)
        json = json.put(XdmAtomicValue("results"), results)

        receiver.output("result", XProcDocument.ofJson(json, stepConfig).with(MediaType.SPARQL_RESULTS_JSON))
    }

    override fun toString(): String = "cx:sparql"
}