package com.xmlcalabash.util.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.spi.DocumentResolverProvider
import java.net.URI

class StandardDocumentResolver: DocumentResolverProvider, DocumentResolver {
    override fun create(): DocumentResolver {
        return this
    }

    override fun resolvableUris(): List<URI> {
        return emptyList()
    }

    override fun resolvableLibraryUris(): List<URI> {
        return emptyList()
    }

    override fun configure(manager: DocumentManager) {
        // nop
    }

    override fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        // This method will never be called, but still...
        return current
    }
}