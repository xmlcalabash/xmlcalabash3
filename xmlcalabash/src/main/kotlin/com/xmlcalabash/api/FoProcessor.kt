package com.xmlcalabash.api

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.io.OutputStream
import java.net.URI

interface FoProcessor {
    fun name(): String
    fun initialize(context: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>)
    fun format(document: XProcDocument, contentType: MediaType, out: OutputStream)
}