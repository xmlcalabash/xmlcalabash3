package com.xmlcalabash.ext.rdf

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerProvider
import net.sf.saxon.Configuration
import org.apache.logging.log4j.kotlin.logger
import javax.activation.MimetypesFileTypeMap

class RdfConfigurer(): Configurer, ConfigurerProvider {
    override fun configure(xmlcalabash: XmlCalabash) {
        // nop
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun configureContentTypes(contentTypes: MutableMap<String, String>, mimeTypes: MimetypesFileTypeMap) {
        val rdfMapping = mapOf(
            "jsonld" to "application/ld+json",
            "n3" to "text/n3",
            "nq" to "application/n-quads",
            "nt" to "application/n-triples",
            "rdf" to "application/rdf+xml",
            "rj" to "application/rdf+json",
            "rq" to "application/sparql-query",
            "srj" to "application/sparql-results+json",
            "srx" to "application/sparql-results+xml",
            "thrift" to "application/rdf+thrift",
            "trig" to "application/trig",
            "trix" to "application/trix+xml", // I invented this one; I didn't find a spec
            "ttl" to "text/turtle"
        )

        for ((ext, contentType) in rdfMapping) {
            contentTypes[ext] = contentType
            if (mimeTypes.getContentType("test.${ext}") == "application/octet-stream") {
                logger.trace { "Assigning default content type to '.${ext}' files: ${contentType}" }
                mimeTypes.addMimeTypes("${contentType} ${ext}")
            }
        }
    }

    override fun create(): Configurer {
        return this
    }
}