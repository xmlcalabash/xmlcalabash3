package com.xmlcalabash.ext.uniqueid

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.spi.DocumentResolverProvider
import com.xmlcalabash.runtime.XProcStepConfiguration
import java.io.IOException
import java.net.URI

class DocumentResolverProvider:  DocumentResolverProvider, DocumentResolver {
    companion object {
        val MAGIC_URI = URI("https://xmlcalabash.com/ext/library/unique-id.xpl")
        val RESOURCE_PATH = "/com/xmlcalabash/ext/unique-id.xpl"
        var library: XProcDocument? = null
    }

    override fun create(): DocumentResolver {
        return this
    }

    override fun configure(manager: DocumentManager) {
        manager.registerPrefix(MAGIC_URI.toString(), this)
    }

    override fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        if (current != null) {
            return current // WAT?
        }

        if (uri != MAGIC_URI) {
            return null // WAT?
        }

        synchronized(Companion) {
            if (library != null) {
                return library
            }

            val loader = DocumentLoader(context, MAGIC_URI)
            val stream = DocumentResolverProvider::class.java.getResourceAsStream(RESOURCE_PATH)
                ?: throw IOException("Failed to find ${RESOURCE_PATH} as a resource")
            library = loader.load(MAGIC_URI, stream, MediaType.XML)
            stream.close()
            return library
        }
    }
}