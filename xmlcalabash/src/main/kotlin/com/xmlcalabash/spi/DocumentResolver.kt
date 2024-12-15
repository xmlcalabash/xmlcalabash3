package com.xmlcalabash.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.XProcStepConfiguration
import java.net.URI

interface DocumentResolver {
    fun configure(manager: DocumentManager)
    fun resolve(context: XProcStepConfiguration, uri: URI, current: XProcDocument?): XProcDocument?
}