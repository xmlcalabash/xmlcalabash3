package com.xmlcalabash.util

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import java.lang.RuntimeException
import java.net.URI

class NopPagedMediaProvider: PagedMediaProvider, PagedMediaManager {
    override fun create(): PagedMediaManager {
        return this
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        return false
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        throw RuntimeException("xmlcalabash does not provide ${formatter}")
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        throw RuntimeException("xmlcalabash does not provide ${formatter}")
    }
}