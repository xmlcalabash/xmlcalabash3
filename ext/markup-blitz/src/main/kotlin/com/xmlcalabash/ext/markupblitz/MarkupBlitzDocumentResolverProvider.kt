package com.xmlcalabash.ext.markupblitz

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class MarkupBlitzDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/markup-blitz.xpl"),
    "/com/xmlcalabash/ext/markup-blitz.xpl")