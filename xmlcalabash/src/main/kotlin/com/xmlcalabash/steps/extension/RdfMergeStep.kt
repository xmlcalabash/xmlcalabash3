package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.RdfConverter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.XdmAtomicValue
import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFWriter
import java.io.ByteArrayOutputStream

class RdfMergeStep(): AbstractRdfStep() {
    override fun run() {
        super.run()

        var dataset = DatasetFactory.create()
        for (doc in queues["source"]!!) {
            dataset = RdfConverter.parseDocument(doc, null, null, dataset)
        }

        val graph = uriBinding(_graph)

        if (graph != null) {
            val model = dataset.getNamedModel(graph.toString())
            dataset = DatasetFactory.create(model)
        }

        val writer = RDFWriter.create().source(dataset).lang(Lang.RDFTHRIFT).build()
        var baos = ByteArrayOutputStream()
        writer.output(baos)

        receiver.output("result", XProcBinaryDocument(baos.toByteArray(), stepConfig,
            DocumentProperties(mapOf(Ns.contentType to XdmAtomicValue(MediaType.RDFTHRIFT.toString())))))
    }

    override fun toString(): String = "cx:rdf-load"
}