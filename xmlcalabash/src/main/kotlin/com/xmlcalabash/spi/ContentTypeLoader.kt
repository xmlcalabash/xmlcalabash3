package com.xmlcalabash.spi

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

interface ContentTypeLoader {
    fun contentTypes(): List<MediaType>
    fun load(context: StepConfiguration, uri: URI?, stream: InputStream, contentType: MediaType, charset: Charset?): XProcDocument
}