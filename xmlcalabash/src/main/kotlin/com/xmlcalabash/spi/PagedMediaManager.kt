package com.xmlcalabash.spi

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import net.sf.saxon.s9api.QName
import java.net.URI

interface PagedMediaManager {
    fun formatters(): List<URI>
    fun formatterSupported(formatter: URI): Boolean
    fun configure(formatter: URI, properties: Map<QName, String>)
    fun formatterAvailable(formatter: URI): Boolean
    fun getCssProcessor(formatter: URI): CssProcessor
    fun getFoProcessor(formatter: URI): FoProcessor
}