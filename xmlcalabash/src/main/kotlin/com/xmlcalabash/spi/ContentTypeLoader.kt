package com.xmlcalabash.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

interface ContentTypeLoader {
    fun contentTypes(): List<MediaType>
    fun load(context: XProcStepConfiguration, uri: URI?, stream: InputStream, contentType: MediaType, charset: Charset?): XProcDocument
}