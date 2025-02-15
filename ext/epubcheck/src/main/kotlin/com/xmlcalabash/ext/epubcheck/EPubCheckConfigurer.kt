package com.xmlcalabash.ext.epubcheck

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerProvider
import net.sf.saxon.Configuration
import org.apache.logging.log4j.kotlin.logger
import javax.activation.MimetypesFileTypeMap

class EPubCheckConfigurer(): Configurer, ConfigurerProvider {
    override fun configure(xmlcalabash: XmlCalabash) {
        // nop
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun configureContentTypes(contentTypes: MutableMap<String, String>, mimeTypes: MimetypesFileTypeMap) {
        val ext = "epub"
        val contentType = "application/epub+zip"
        contentTypes[ext] = contentType
        if (mimeTypes.getContentType("test.${ext}") == "application/octet-stream") {
            logger.trace { "Assigning default content type to '.${ext}' files: ${contentType}" }
            mimeTypes.addMimeTypes("${contentType} ${ext}")
        }
    }

    override fun create(): Configurer {
        return this
    }
}