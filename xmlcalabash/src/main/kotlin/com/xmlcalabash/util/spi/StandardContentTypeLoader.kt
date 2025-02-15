package com.xmlcalabash.util.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.spi.ContentTypeLoader
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class StandardContentTypeLoader(): ContentTypeLoader {
    override fun contentTypes(): List<MediaType> {
        return emptyList()
    }

    override fun load(context: XProcStepConfiguration, uri: URI?, stream: InputStream, contentType: MediaType, charset: Charset?): XProcDocument {
        throw IllegalArgumentException("The StandardContentTypeLoader doesn't support any content types")
    }
}