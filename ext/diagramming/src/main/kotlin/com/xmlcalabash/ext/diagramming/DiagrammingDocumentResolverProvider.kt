package com.xmlcalabash.ext.diagramming

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class DiagrammingDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/diagramming.xpl"),
    "/com/xmlcalabash/ext/diagramming.xpl")