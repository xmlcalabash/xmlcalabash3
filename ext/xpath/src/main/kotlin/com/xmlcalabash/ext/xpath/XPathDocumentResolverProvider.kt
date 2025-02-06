package com.xmlcalabash.ext.xpath

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class XPathDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/xpath.xpl"),
    "/com/xmlcalabash/ext/xpath.xpl")