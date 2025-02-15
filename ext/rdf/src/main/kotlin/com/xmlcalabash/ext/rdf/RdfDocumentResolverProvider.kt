package com.xmlcalabash.ext.rdf

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class RdfDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/rdf.xpl"),
    "/com/xmlcalabash/ext/rdf.xpl")