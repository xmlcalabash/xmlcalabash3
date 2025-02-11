package com.xmlcalabash.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

interface ContentTypeConverter {
    fun conversions(): List<Pair<MediaType, MediaType>>
    fun convert(stepConfig: XProcStepConfiguration, doc: XProcDocument, convertTo: MediaType, serialization: Map<QName, XdmValue>): XProcDocument
}