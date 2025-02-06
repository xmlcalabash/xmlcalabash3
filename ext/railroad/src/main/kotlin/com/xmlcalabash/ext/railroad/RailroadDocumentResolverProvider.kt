package com.xmlcalabash.ext.rr

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class RailroadDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/railroad.xpl"),
    "/com/xmlcalabash/ext/railroad.xpl")