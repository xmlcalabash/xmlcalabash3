package com.xmlcalabash.ext.uniqueid

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class UniqueIdDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/unique-id.xpl"),
    "/com/xmlcalabash/ext/unique-id.xpl")