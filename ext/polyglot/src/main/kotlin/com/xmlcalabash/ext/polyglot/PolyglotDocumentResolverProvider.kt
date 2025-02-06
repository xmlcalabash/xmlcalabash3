package com.xmlcalabash.ext.polyglot

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class PolyglotDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/polyglot.xpl"),
    "/com/xmlcalabash/ext/polyglot.xpl")