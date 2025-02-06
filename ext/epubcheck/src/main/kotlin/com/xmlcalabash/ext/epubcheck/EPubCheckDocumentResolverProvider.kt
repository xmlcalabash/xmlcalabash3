package com.xmlcalabash.ext.epubcheck

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class EPubCheckDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/epubcheck.xpl"),
    "/com/xmlcalabash/ext/epubcheck.xpl")