package com.xmlcalabash.spi

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import java.net.URI

interface PagedMediaManager {
    fun formatterAvailable(formatter: URI): Boolean
    fun getCssProcessor(formatter: URI): CssProcessor
    fun getFoProcessor(formatter: URI): FoProcessor
}