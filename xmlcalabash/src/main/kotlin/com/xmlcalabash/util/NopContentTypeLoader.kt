package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.spi.ContentTypeLoader
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class NopContentTypeLoader(): ContentTypeLoader {
    override fun contentTypes(): List<MediaType> {
        return emptyList()
    }

    override fun load(
        uri: URI?,
        stream: InputStream,
        contentType: MediaType,
        charset: Charset?
    ): XProcDocument {
        throw IllegalArgumentException("The NopContentTypeLoader doesn't support any content types")
    }
}