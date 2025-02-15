package com.xmlcalabash.ext.rdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.FileUtils
import com.xmlcalabash.util.SaxonTreeBuilder
import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFWriter
import java.io.ByteArrayOutputStream

class RdfGraphStep(): AbstractRdfStep() {
    override fun run() {
        super.run()

        val graph = uriBinding(_graph)

        val doc = queues["source"]!!.first()
        val dataset = parseDocument(doc, null, null)

        val model = if (graph != null) {
            dataset.getNamedModel(graph.toString())
        } else {
            dataset.defaultModel
        }

        val writer = RDFWriter.create().source(model).lang(Lang.RDFTHRIFT).build()
        val baos = ByteArrayOutputStream()
        writer.output(baos)

        receiver.output("result", XProcBinaryDocument(baos.toByteArray(), stepConfig))
    }

    override fun toString(): String = "cx:rdf-graph"
}