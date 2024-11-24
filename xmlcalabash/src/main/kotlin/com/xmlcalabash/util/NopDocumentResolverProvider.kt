package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.spi.DocumentResolverProvider
import java.net.URI

class NopDocumentResolverProvider: DocumentResolverProvider, DocumentResolver {
    override fun create(): DocumentResolver {
        return this
    }

    override fun configure(manager: DocumentManager) {
        // manager.registerPrefix("file:/Volumes/Documents/xsl/", this)
        // nop
    }

    override fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument? {
        // This method will never be called, but still...
        return current
    }
}