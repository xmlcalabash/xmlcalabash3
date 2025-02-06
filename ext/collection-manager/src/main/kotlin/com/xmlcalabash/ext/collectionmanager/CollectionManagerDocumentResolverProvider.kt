package com.xmlcalabash.ext.collectionmanager

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class CollectionManagerDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/collection-manager.xpl"),
    "/com/xmlcalabash/ext/collection-manager.xpl")
