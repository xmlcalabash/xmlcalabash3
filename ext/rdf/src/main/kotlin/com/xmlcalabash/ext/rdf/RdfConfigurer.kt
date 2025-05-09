package com.xmlcalabash.ext.rdf

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerProvider
import net.sf.saxon.Configuration

class RdfConfigurer(): Configurer, ConfigurerProvider {
    override fun configure(builder: XmlCalabashBuilder) {
        builder.addMimeType("application/ld+json", listOf("jsonld"))
        builder.addMimeType("text/n3", listOf("n3"))
        builder.addMimeType("application/n-quads", listOf("nq"))
        builder.addMimeType("application/n-triples", listOf("nt"))
        builder.addMimeType("application/rdf+xml", listOf("rdf"))
        builder.addMimeType("application/rdf+json", listOf("rj"))
        builder.addMimeType("application/sparql-query", listOf("rq"))
        builder.addMimeType("application/sparql-results+json", listOf("srj"))
        builder.addMimeType("application/sparql-results+xml", listOf("srx"))
        builder.addMimeType("application/rdf+thrift", listOf("thrift"))
        builder.addMimeType("application/trig", listOf("trig"))
        builder.addMimeType("application/trix+xml", listOf("trix")) // I invented this one; I didn't find a spec
        builder.addMimeType("text/turtle", listOf("ttl"))
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun create(): Configurer {
        return this
    }
}