package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.spi.DocumentResolver
import java.io.IOException
import java.net.URI

class InternalDocumentResolver(): DocumentResolver {
    companion object {
        val XVRL_TO_TEXT = URI("https://xmlcalabash.com/stylesheet/xvrl-to-text.xsl")
        val URI_MAP = mapOf(
            XVRL_TO_TEXT to "/com/xmlcalabash/format-xvrl.xsl"
        )
        private val document_map = mutableMapOf<URI, XProcDocument>()
    }

    override fun resolvableUris(): List<URI> {
        return URI_MAP.keys.toList()
    }

    override fun configure(manager: DocumentManager) {
        for ((uri, _) in URI_MAP) {
            manager.registerPrefix(uri.toString(), this)
        }
    }

    override fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        if (current != null) {
            return current // WAT?
        }

        if (uri !in URI_MAP) {
            return null // WAT?
        }

        synchronized(Companion) {
            if (uri in document_map) {
                return document_map[uri]!!
            }

            val path = URI_MAP[uri]!!
            val loader = DocumentLoader(context, uri)
            val stream = InternalDocumentResolver::class.java.getResourceAsStream(path)
                ?: throw IOException("Failed to find ${path} as a resource")
            val document = loader.load(uri, stream, MediaType.XML)
            stream.close()
            document_map[uri] = document
            return document
        }
    }
}