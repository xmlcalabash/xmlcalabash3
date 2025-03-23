package com.xmlcalabash.ext.trang

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class TrangDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/trang.xpl"),
    "/com/xmlcalabash/ext/trang.xpl")