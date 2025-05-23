package com.xmlcalabash.util.spi

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.spi.DocumentResolverProvider
import java.io.IOException
import java.net.URI

class StandardDocumentResolver: DocumentResolverProvider, DocumentResolver {
    private val uriMapping = mapOf(
        URI("https://xmlcalabash.com/ext/library/asciidoctor.xpl") to "/com/xmlcalabash/ext/asciidoctor.xpl",
        URI("https://xmlcalabash.com/ext/library/cache.xpl") to "/com/xmlcalabash/ext/cache.xpl",
        URI("https://xmlcalabash.com/ext/library/collection-manager.xpl") to "/com/xmlcalabash/ext/collection-manager.xpl",
        URI("https://xmlcalabash.com/ext/library/diagramming.xpl") to "/com/xmlcalabash/ext/diagramming.xpl",
        URI("https://xmlcalabash.com/ext/library/ebnf-convert.xpl") to "/com/xmlcalabash/ext/ebnf-convert.xpl",
        URI("https://xmlcalabash.com/ext/library/epubcheck.xpl") to "/com/xmlcalabash/ext/epubcheck.xpl",
        URI("https://xmlcalabash.com/ext/library/find.xpl") to "/com/xmlcalabash/ext/find.xpl",
        URI("https://xmlcalabash.com/ext/library/json-patch.xpl") to "/com/xmlcalabash/ext/json-patch.xpl",
        URI("https://xmlcalabash.com/ext/library/jsonpath.xpl") to "/com/xmlcalabash/ext/jsonpath.xpl",
        URI("https://xmlcalabash.com/ext/library/markup-blitz.xpl") to "/com/xmlcalabash/ext/markup-blitz.xpl",
        URI("https://xmlcalabash.com/ext/library/metadata-extractor.xpl") to "/com/xmlcalabash/ext/metadata-extractor.xpl",
        URI("https://xmlcalabash.com/ext/library/pipeline-messages.xpl") to "/com/xmlcalabash/ext/pipeline-messages.xpl",
        URI("https://xmlcalabash.com/ext/library/polyglot.xpl") to "/com/xmlcalabash/ext/polyglot.xpl",
        URI("https://xmlcalabash.com/ext/library/railroad.xpl") to "/com/xmlcalabash/ext/railroad.xpl",
        URI("https://xmlcalabash.com/ext/library/rdf.xpl") to "/com/xmlcalabash/ext/rdf.xpl",
        URI("https://xmlcalabash.com/ext/library/selenium.xpl") to "/com/xmlcalabash/ext/selenium.xpl",
        URI("https://xmlcalabash.com/ext/library/trang.xpl") to "/com/xmlcalabash/ext/trang.xpl",
        URI("https://xmlcalabash.com/ext/library/unique-id.xpl") to "/com/xmlcalabash/ext/unique-id.xpl",
        URI("https://xmlcalabash.com/ext/library/wait-for-update.xpl") to "/com/xmlcalabash/ext/wait-for-update.xpl",
        URI("https://xmlcalabash.com/ext/library/xmlunit.xpl") to "/com/xmlcalabash/ext/xmlunit.xpl",
        URI("https://xmlcalabash.com/ext/library/xpath.xpl") to "/com/xmlcalabash/ext/xpath.xpl"
    )
    private val libraryMapping = mutableMapOf<URI, XProcDocument>()

    override fun create(): DocumentResolver {
        return this
    }

    override fun resolvableUris(): List<URI> {
        return uriMapping.keys.toList()
    }

    override fun resolvableLibraryUris(): List<URI> {
        return uriMapping.keys.toList()
    }

    override fun configure(manager: DocumentManager) {
        for (magicUri in uriMapping.keys) {
            manager.registerPrefix(magicUri.toString(), this)
        }
    }

    override fun resolve(context: StepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        if (current != null) {
            return current // WAT?
        }

        if (uri !in uriMapping) {
            return null // WAT?
        }

        synchronized(this) {
            if (uri !in libraryMapping) {
                val resourcePath = uriMapping[uri]!!
                val loader = DocumentLoader(context, uri)
                val stream = this::class.java.getResourceAsStream(resourcePath)
                    ?: throw IOException("Failed to find ${resourcePath} as a resource")
                val library = loader.load(stream, MediaType.XML)
                stream.close()
                libraryMapping[uri] = library
            }
            return libraryMapping[uri]!!
        }
    }
}