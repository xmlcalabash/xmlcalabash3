package com.xmlcalabash.ext.find

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class FindDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/find.xpl"),
    "/com/xmlcalabash/ext/find.xpl")