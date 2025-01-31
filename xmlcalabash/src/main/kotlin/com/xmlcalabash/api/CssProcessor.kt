package com.xmlcalabash.api

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.io.OutputStream
import java.net.URI

interface CssProcessor {
    fun name(): String
    fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>)
    fun format(document: XProcDocument, contentType: MediaType, out: OutputStream)
    fun addStylesheet(document: XProcDocument)
}