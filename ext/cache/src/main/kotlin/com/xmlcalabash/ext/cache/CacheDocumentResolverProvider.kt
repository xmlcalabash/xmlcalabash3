package com.xmlcalabash.ext.cache

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class CacheDocumentResolverProvider:  SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/cache.xpl"),
    "/com/xmlcalabash/ext/cache.xpl")