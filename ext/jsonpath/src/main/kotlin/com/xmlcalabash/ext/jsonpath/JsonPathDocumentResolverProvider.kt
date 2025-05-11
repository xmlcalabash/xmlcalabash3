package com.xmlcalabash.ext.jsonpath

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class JsonPathDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/jsonpath.xpl"),
    "/com/xmlcalabash/ext/jsonpath.xpl")