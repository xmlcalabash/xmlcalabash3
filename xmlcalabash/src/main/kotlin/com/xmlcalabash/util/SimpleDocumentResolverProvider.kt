package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.spi.DocumentResolverProvider
import java.io.IOException
import java.net.URI

open class SimpleDocumentResolverProvider(val magicUri: URI, val resourcePath: String):  DocumentResolverProvider, DocumentResolver {
    private var library: XProcDocument? = null

    override fun resolvableUris(): List<URI> {
        return listOf(magicUri)
    }

    override fun resolvableLibraryUris(): List<URI> {
        return listOf(magicUri)
    }

    override fun create(): DocumentResolver {
        return this
    }

    override fun configure(manager: DocumentManager) {
        manager.registerPrefix(magicUri.toString(), this)
    }

    override fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        if (current != null) {
            return current // WAT?
        }

        if (uri != magicUri) {
            return null // WAT?
        }

        synchronized(this) {
            if (library != null) {
                return library
            }

            val loader = DocumentLoader(context, magicUri)
            val stream = this::class.java.getResourceAsStream(resourcePath)
                ?: throw IOException("Failed to find ${resourcePath} as a resource")
            library = loader.load(stream, MediaType.XML)
            stream.close()
            return library
        }
    }
}