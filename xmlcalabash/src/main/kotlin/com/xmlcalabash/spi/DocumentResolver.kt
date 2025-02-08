package com.xmlcalabash.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.XProcStepConfiguration
import java.net.URI

interface DocumentResolver {
    fun resolvableUris(): List<URI>
    fun resolvableLibraryUris(): List<URI>
    fun configure(manager: DocumentManager)
    fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument?
}